package br.com.streetplay.features.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.streetplay.core.common.moved
import br.com.streetplay.core.storage.PrefsStore
import br.com.streetplay.data.AppRepository
import br.com.streetplay.data.StreamingCatalog
import br.com.streetplay.domain.HomeEntry
import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.domain.ShortcutState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(private val repo: AppRepository, private val prefs: PrefsStore) : ViewModel() {
    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    private val _shortcuts = MutableStateFlow<List<ShortcutState>>(emptyList())
    val shortcuts: StateFlow<List<ShortcutState>> = _shortcuts.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val favorites: StateFlow<List<String>> =
        repo.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recents: StateFlow<List<String>> =
        repo.recents(12).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Itens fixados na Home (persistem ao fechar o app). */
    val homePins: StateFlow<List<String>> = prefs.homePins.map { it ?: StreamingCatalog.DEFAULT_PINS }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StreamingCatalog.DEFAULT_PINS)

    val homeEntries: StateFlow<List<HomeEntry>> = combine(homePins, _apps, _shortcuts) { pins, apps, shortcuts ->
        pins.mapNotNull { pin -> resolvePin(pin, apps, shortcuts) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    /** Chamado em onResume: apps podem ter sido instalados/removidos. */
    fun refresh() = viewModelScope.launch {
        _apps.value = repo.installedApps()
        _shortcuts.value = repo.shortcuts()
        _loading.value = false
    }

    fun open(pkg: String) = viewModelScope.launch {
        if (!repo.launch(pkg)) _messages.emit("Não foi possível abrir este aplicativo.")
    }

    /** "Loja" vai direto para a Google Play Store do aparelho. */
    fun openPlayStore() = viewModelScope.launch {
        val pkg = StreamingCatalog.PLAY_STORE_PACKAGE
        if (!repo.isInstalled(pkg)) _messages.emit("Este aparelho não tem a Google Play Store.")
        else if (!repo.launch(pkg)) _messages.emit("Não foi possível abrir a Play Store.")
    }

    /** Globoplay (app oficial): abre se estiver instalado; senão leva à ficha na loja. */
    fun openGloboplay() {
        val g = StreamingCatalog.all.first { it.id == "globoplay" }
        val pkg = StreamingCatalog.resolve(g, repo::isInstalled)
        if (pkg != null) open(pkg) else openStoreListing(g.packages.first())
    }

    fun openStoreListing(pkg: String) {
        if (!repo.openStoreListing(pkg)) _messages.tryEmit("Este aparelho não tem uma loja de aplicativos instalada.")
    }

    fun togglePin(id: String) {
        val cur = homePins.value
        viewModelScope.launch { prefs.setHomePins(if (id in cur) cur - id else cur + id) }
    }

    fun movePin(id: String, delta: Int) {
        val cur = homePins.value
        val moved = cur.moved(cur.indexOf(id), delta)
        if (moved !== cur) viewModelScope.launch { prefs.setHomePins(moved) }
    }

    fun toggleFavorite(pkg: String) = viewModelScope.launch { repo.toggleFavorite(pkg) }
    fun moveFavorite(pkg: String, delta: Int) = viewModelScope.launch { repo.moveFavorite(pkg, delta) }
    suspend fun loadIcon(pkg: String): ImageBitmap? = repo.icon(pkg)
    suspend fun loadBanner(pkg: String): ImageBitmap? = repo.banner(pkg)
}

internal fun resolvePin(pin: String, apps: List<InstalledApp>, shortcuts: List<ShortcutState>): HomeEntry? = when {
    pin.startsWith("sc:") -> {
        val id = pin.removePrefix("sc:")
        StreamingCatalog.all.firstOrNull { it.id == id }?.let { sc ->
            val st = shortcuts.firstOrNull { it.shortcut.id == id }
            HomeEntry(pin, sc.name, st?.resolvedPackage ?: sc.packages.first(), st?.installed == true, sc.brandColor, sc.packages.first())
        }
    }
    pin.startsWith("pkg:") -> {
        val pkg = pin.removePrefix("pkg:")
        apps.firstOrNull { it.packageName == pkg }?.let { HomeEntry(pin, it.label, it.packageName, true, 0xFFFFFFFF, null) }
    }
    else -> null
}
