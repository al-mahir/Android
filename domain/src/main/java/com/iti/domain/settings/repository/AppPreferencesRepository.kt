package com.iti.domain.settings.repository

import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow


interface AppPreferencesRepository {

    val preferences: Flow<AppPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setRemindersEnabled(enabled: Boolean)

    suspend fun setErrorSoundsEnabled(enabled: Boolean)

    suspend fun setDataSaverEnabled(enabled: Boolean)
}
