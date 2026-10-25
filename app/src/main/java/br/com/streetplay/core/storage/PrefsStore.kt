package br.com.streetplay.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.streetplay.domain.AppMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "street_play_prefs")

data class UserPrefs(val mode: AppMode = AppMode.STREET, val animations: Boolean = true)

class PrefsStore(private val context: Context) {
    private object Keys {
        val MODE = intPreferencesKey("mode")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val LIVE_FAVORITES = stringSetPreferencesKey("live_favorites")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map {
        UserPrefs(AppMode.entries.getOrElse(it[Keys.MODE] ?: 0) { AppMode.STREET }, it[Keys.ANIMATIONS] ?: true)
    }

    val liveFavorites: Flow<Set<String>> = context.dataStore.data.map { it[Keys.LIVE_FAVORITES] ?: emptySet() }

    suspend fun setMode(mode: AppMode) { context.dataStore.edit { it[Keys.MODE] = mode.ordinal } }
    suspend fun setAnimations(on: Boolean) { context.dataStore.edit { it[Keys.ANIMATIONS] = on } }
    suspend fun toggleLiveFavorite(channelId: String) {
        context.dataStore.edit {
            val cur = it[Keys.LIVE_FAVORITES] ?: emptySet()
            it[Keys.LIVE_FAVORITES] = if (channelId in cur) cur - channelId else cur + channelId
        }
    }
}
