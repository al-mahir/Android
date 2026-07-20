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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings_prefs",
)

class AppPreferencesDataStore(context: Context) {

    private val dataStore = context.applicationContext.appSettingsDataStore

    val preferences: Flow<AppPreferences> = dataStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[KEY_THEME_MODE].toThemeMode(),
            language = AppLanguage.fromTag(prefs[KEY_LANGUAGE]),
            remindersEnabled = prefs[KEY_REMINDERS] ?: true,
            errorSoundsEnabled = prefs[KEY_ERROR_SOUNDS] ?: true,
            dataSaverEnabled = prefs[KEY_DATA_SAVER] ?: false,
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

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_LANGUAGE = stringPreferencesKey("language_tag")
        val KEY_REMINDERS = booleanPreferencesKey("reminders_enabled")
        val KEY_ERROR_SOUNDS = booleanPreferencesKey("error_sounds_enabled")
        val KEY_DATA_SAVER = booleanPreferencesKey("data_saver_enabled")
    }
}
