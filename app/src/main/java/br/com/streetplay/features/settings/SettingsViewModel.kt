package br.com.streetplay.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.streetplay.AppContainer
import br.com.streetplay.core.storage.PrefsStore
import br.com.streetplay.core.storage.XtreamAccountStore
import br.com.streetplay.data.remote.XtreamApi
import br.com.streetplay.data.remote.XtreamCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import br.com.streetplay.core.storage.UserPrefs
import br.com.streetplay.domain.AccentTheme
import br.com.streetplay.domain.AppMode
import br.com.streetplay.features.home.DiscoverViewModel
import br.com.streetplay.features.home.LauncherViewModel
import br.com.streetplay.features.live.LiveViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val store: PrefsStore, private val xtreamStore: XtreamAccountStore, private val xtreamApi: XtreamApi) : ViewModel() {
    private val _iptvConnected = MutableStateFlow(false)
    val iptvConnected = _iptvConnected.asStateFlow()
    private val _iptvChecking = MutableStateFlow(xtreamStore.load() != null)
    val iptvChecking = _iptvChecking.asStateFlow()
    private val _iptvMessage = MutableStateFlow("")
    val iptvMessage = _iptvMessage.asStateFlow()

    init {
        val saved = xtreamStore.load()
        if (saved != null) viewModelScope.launch {
            _iptvChecking.value = true
            try {
                xtreamApi.authenticate(saved)
                _iptvConnected.value = true
                _iptvMessage.value = "Conta IPTV validada."
            } catch (e: Exception) {
                _iptvConnected.value = false
                _iptvMessage.value = e.message ?: "Sua conta precisa ser conectada novamente."
            } finally { _iptvChecking.value = false }
        }
    }

    fun saveIptv(server: String, username: String, password: String) = viewModelScope.launch {
        _iptvChecking.value = true
        _iptvMessage.value = "Validando dados com o servidor IPTV…"
        try {
            val account = XtreamCredentials(server.trim().trimEnd('/'), username.trim(), password)
            xtreamApi.authenticate(account)
            xtreamStore.save(account)
            _iptvConnected.value = true
            _iptvMessage.value = "Acesso liberado! Conta validada com sucesso."
        } catch (e: Exception) {
            _iptvConnected.value = false
            _iptvMessage.value = e.message ?: "Não foi possível conectar."
        } finally { _iptvChecking.value = false }
    }
    fun disconnectIptv() { xtreamStore.clear(); _iptvConnected.value = false; _iptvMessage.value = "Conta IPTV desconectada." }
    val prefs: StateFlow<UserPrefs> = store.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, UserPrefs())
    fun setMode(mode: AppMode) = viewModelScope.launch { store.setMode(mode) }
    fun setAccent(theme: AccentTheme) = viewModelScope.launch { store.setAccent(theme) }
    fun setAnimations(on: Boolean) = viewModelScope.launch { store.setAnimations(on) }
}

class AppViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LauncherViewModel::class.java) -> LauncherViewModel(c.repository, c.prefs) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(c.prefs, c.xtreamAccountStore, c.xtreamApi) as T
        modelClass.isAssignableFrom(DiscoverViewModel::class.java) -> DiscoverViewModel(c.catalog) as T
        modelClass.isAssignableFrom(LiveViewModel::class.java) -> LiveViewModel(c.catalog, c.prefs, c.xtreamAccountStore, c.xtreamApi) as T
        else -> error("ViewModel desconhecido: ${modelClass.name}")
    }
}
