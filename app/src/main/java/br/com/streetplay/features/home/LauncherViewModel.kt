package br.com.streetplay.features.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.streetplay.data.AppRepository
import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.domain.ShortcutState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(private val repo: AppRepository) : ViewModel() {
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

    fun openStoreListing(pkg: String) {
        if (!repo.openStoreListing(pkg)) _messages.tryEmit("Este aparelho não tem uma loja de aplicativos instalada.")
    }

    fun toggleFavorite(pkg: String) = viewModelScope.launch { repo.toggleFavorite(pkg) }
    fun moveFavorite(pkg: String, delta: Int) = viewModelScope.launch { repo.moveFavorite(pkg, delta) }
    suspend fun loadIcon(pkg: String): ImageBitmap? = repo.icon(pkg)
}
