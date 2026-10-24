package br.com.streetplay.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "street_play_prefs")

data class UserPrefs(val wallpaper: Int = 0, val accent: Int = 0, val animations: Boolean = true)

class PrefsStore(private val context: Context) {
    private object Keys {
        val WALLPAPER = intPreferencesKey("wallpaper")
        val ACCENT = intPreferencesKey("accent")
        val ANIMATIONS = booleanPreferencesKey("animations")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map {
        UserPrefs(it[Keys.WALLPAPER] ?: 0, it[Keys.ACCENT] ?: 0, it[Keys.ANIMATIONS] ?: true)
    }

    suspend fun setWallpaper(i: Int) { context.dataStore.edit { it[Keys.WALLPAPER] = i } }
    suspend fun setAccent(i: Int) { context.dataStore.edit { it[Keys.ACCENT] = i } }
    suspend fun setAnimations(on: Boolean) { context.dataStore.edit { it[Keys.ANIMATIONS] = on } }
}
