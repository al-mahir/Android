package com.iti.data.settings.repository

import com.iti.data.settings.local.AppPreferencesDataStore
import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.settings.model.ThemeMode
import com.iti.domain.settings.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow

class AppPreferencesRepositoryImpl(
    private val dataStore: AppPreferencesDataStore,
) : AppPreferencesRepository {

    override val preferences: Flow<AppPreferences> = dataStore.preferences

    override suspend fun setThemeMode(mode: ThemeMode) = dataStore.setThemeMode(mode)

    override suspend fun setLanguage(language: AppLanguage) = dataStore.setLanguage(language)

    override suspend fun setRemindersEnabled(enabled: Boolean) =
        dataStore.setRemindersEnabled(enabled)

    override suspend fun setErrorSoundsEnabled(enabled: Boolean) =
        dataStore.setErrorSoundsEnabled(enabled)

    override suspend fun setDataSaverEnabled(enabled: Boolean) =
        dataStore.setDataSaverEnabled(enabled)
}
