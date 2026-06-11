package com.aistudio.lensora.gallery.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lensora_settings")

class SettingsManager(private val context: Context) {

    companion object {
        val VAULT_PIN = stringPreferencesKey("vault_pin")
        val SLIDESHOW_SPEED = intPreferencesKey("slideshow_speed") // in ms
        val APP_THEME = stringPreferencesKey("app_theme") // "system", "light", "dark"
        val CUSTOM_ALBUM_FOLDERS = stringPreferencesKey("custom_album_folders") // Comma-separated paths
    }

    val vaultPinFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[VAULT_PIN]
    }

    val slideshowSpeedFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SLIDESHOW_SPEED] ?: 3000 // 3 seconds default
    }

    val appThemeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_THEME] ?: "system"
    }

    val customAlbumFoldersFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[CUSTOM_ALBUM_FOLDERS] ?: ""
        if (raw.isEmpty()) emptyList() else raw.split(",")
    }

    suspend fun saveVaultPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[VAULT_PIN] = pin
        }
    }

    suspend fun clearVaultPin() {
        context.dataStore.edit { preferences ->
            preferences.remove(VAULT_PIN)
        }
    }

    suspend fun saveSlideshowSpeed(speedMs: Int) {
        context.dataStore.edit { preferences ->
            preferences[SLIDESHOW_SPEED] = speedMs
        }
    }

    suspend fun saveAppTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme
        }
    }

    suspend fun addCustomAlbumFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_ALBUM_FOLDERS] ?: ""
            val list = if (current.isEmpty()) mutableListOf() else current.split(",").toMutableList()
            if (!list.contains(folderPath)) {
                list.add(folderPath)
                preferences[CUSTOM_ALBUM_FOLDERS] = list.joinToString(",")
            }
        }
    }

    suspend fun removeCustomAlbumFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_ALBUM_FOLDERS] ?: ""
            if (current.isNotEmpty()) {
                val list = current.split(",").toMutableList()
                if (list.remove(folderPath)) {
                    preferences[CUSTOM_ALBUM_FOLDERS] = list.joinToString(",")
                }
            }
        }
    }
}
