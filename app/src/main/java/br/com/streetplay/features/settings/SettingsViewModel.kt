package br.com.streetplay.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.streetplay.AppContainer
import br.com.streetplay.core.storage.PrefsStore
import br.com.streetplay.core.storage.UserPrefs
import br.com.streetplay.features.home.LauncherViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val store: PrefsStore) : ViewModel() {
    val prefs: StateFlow<UserPrefs> = store.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, UserPrefs())
    fun setWallpaper(i: Int) = viewModelScope.launch { store.setWallpaper(i) }
    fun setAccent(i: Int) = viewModelScope.launch { store.setAccent(i) }
    fun setAnimations(on: Boolean) = viewModelScope.launch { store.setAnimations(on) }
}

class AppViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LauncherViewModel::class.java) -> LauncherViewModel(c.repository) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(c.prefs) as T
        else -> error("ViewModel desconhecido: ${modelClass.name}")
    }
}
