package br.com.streetplay.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.streetplay.domain.AccentTheme
import br.com.streetplay.domain.AppMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "street_play_prefs")

data class UserPrefs(
    val mode: AppMode = AppMode.STREET,
    val animations: Boolean = true,
    val accent: AccentTheme = AccentTheme.ORANGE,
)

class PrefsStore(private val context: Context) {
    private object Keys {
        val MODE = intPreferencesKey("mode")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val ACCENT = intPreferencesKey("accent_theme")
        val HOME_PINS = stringPreferencesKey("home_pins")
        val LIVE_FAVORITES = stringSetPreferencesKey("live_favorites")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map {
        UserPrefs(
            AppMode.entries.getOrElse(it[Keys.MODE] ?: 0) { AppMode.STREET },
            it[Keys.ANIMATIONS] ?: true,
            AccentTheme.entries.getOrElse(it[Keys.ACCENT] ?: 0) { AccentTheme.ORANGE },
        )
    }

    val liveFavorites: Flow<Set<String>> = context.dataStore.data.map { it[Keys.LIVE_FAVORITES] ?: emptySet() }

    /** Itens fixados na Home, em ordem. null = nunca personalizou (usa o padrão). */
    val homePins: Flow<List<String>?> = context.dataStore.data.map { p ->
        p[Keys.HOME_PINS]?.split('\n')?.filter { it.isNotBlank() }
    }

    suspend fun setMode(mode: AppMode) { context.dataStore.edit { it[Keys.MODE] = mode.ordinal } }
    suspend fun setAnimations(on: Boolean) { context.dataStore.edit { it[Keys.ANIMATIONS] = on } }
    suspend fun setAccent(theme: AccentTheme) { context.dataStore.edit { it[Keys.ACCENT] = theme.ordinal } }
    suspend fun setHomePins(pins: List<String>) { context.dataStore.edit { it[Keys.HOME_PINS] = pins.joinToString("\n") } }
    suspend fun toggleLiveFavorite(channelId: String) {
        context.dataStore.edit {
            val cur = it[Keys.LIVE_FAVORITES] ?: emptySet()
            it[Keys.LIVE_FAVORITES] = if (channelId in cur) cur - channelId else cur + channelId
        }
    }
}
