package com.iti.data.settings.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.settings.model.ThemeMode
import com.iti.domain.model.User
import kotlinx.coroutines.flow.Flow
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings_prefs",
)

class AppPreferencesDataStore(context: Context) {

    private val dataStore = context.applicationContext.appSettingsDataStore

    val preferences: Flow<AppPreferences> = dataStore.data.map { prefs ->
        val userId = prefs[KEY_USER_ID]
        val user = if (userId != null) {
            User(
                id = userId,
                displayName = prefs[KEY_USER_DISPLAY_NAME] ?: "",
                initials = prefs[KEY_USER_INITIALS] ?: "",
                avatarUrl = prefs[KEY_USER_AVATAR_URL],
                email = prefs[KEY_USER_EMAIL] ?: "",
                joinedAtEpochMillis = prefs[KEY_USER_JOINED_AT] ?: 0L,
            )
        } else {
            null
        }
        
        AppPreferences(
            themeMode = prefs[KEY_THEME_MODE].toThemeMode(),
            language = AppLanguage.fromTag(prefs[KEY_LANGUAGE]),
            remindersEnabled = prefs[KEY_REMINDERS] ?: true,
            errorSoundsEnabled = prefs[KEY_ERROR_SOUNDS] ?: true,
            dataSaverEnabled = prefs[KEY_DATA_SAVER] ?: false,
            user = user,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[KEY_LANGUAGE] = language.tag }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_REMINDERS] = enabled }
    }

    suspend fun setErrorSoundsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ERROR_SOUNDS] = enabled }
    }

    suspend fun setDataSaverEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_DATA_SAVER] = enabled }
    }

    private fun String?.toThemeMode(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM
        
    suspend fun saveUser(user: User) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = user.id
            prefs[KEY_USER_DISPLAY_NAME] = user.displayName
            prefs[KEY_USER_INITIALS] = user.initials
            val avatarUrl = user.avatarUrl
            if (avatarUrl != null) {
                prefs[KEY_USER_AVATAR_URL] = avatarUrl
            } else {
                prefs.remove(KEY_USER_AVATAR_URL)
            }
            prefs[KEY_USER_EMAIL] = user.email
            prefs[KEY_USER_JOINED_AT] = user.joinedAtEpochMillis
        }
    }
    
    suspend fun clearUser() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_DISPLAY_NAME)
            prefs.remove(KEY_USER_INITIALS)
            prefs.remove(KEY_USER_AVATAR_URL)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_JOINED_AT)
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_LANGUAGE = stringPreferencesKey("language_tag")
        val KEY_REMINDERS = booleanPreferencesKey("reminders_enabled")
        val KEY_ERROR_SOUNDS = booleanPreferencesKey("error_sounds_enabled")
        val KEY_DATA_SAVER = booleanPreferencesKey("data_saver_enabled")
        
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val KEY_USER_INITIALS = stringPreferencesKey("user_initials")
        val KEY_USER_AVATAR_URL = stringPreferencesKey("user_avatar_url")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_JOINED_AT = longPreferencesKey("user_joined_at")
    }
}
