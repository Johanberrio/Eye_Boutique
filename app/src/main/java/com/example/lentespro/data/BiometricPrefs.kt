package com.example.lentespro.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "biometric_prefs")

class BiometricPrefs(private val context: Context) {

    private val KEY_ENABLED = booleanPreferencesKey("biometric_enabled")
    private val KEY_LAST_UID = stringPreferencesKey("last_uid")
    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode") // ✅ Nuevo: Modo oscuro

    val enabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ENABLED] ?: false }
    val lastUidFlow: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_UID] }
    
    // ✅ Nuevo: Flow para observar el modo oscuro
    val darkModeFlow: Flow<Boolean?> = context.dataStore.data.map { it[KEY_DARK_MODE] }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setLastUid(uid: String?) {
        context.dataStore.edit {
            if (uid == null) it.remove(KEY_LAST_UID) else it[KEY_LAST_UID] = uid
        }
    }

    // ✅ Nuevo: Guardar preferencia de modo oscuro
    suspend fun setDarkMode(enabled: Boolean?) {
        context.dataStore.edit {
            if (enabled == null) it.remove(KEY_DARK_MODE)
            else it[KEY_DARK_MODE] = enabled
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(KEY_ENABLED)
            it.remove(KEY_LAST_UID)
            it.remove(KEY_DARK_MODE)
        }
    }
}
