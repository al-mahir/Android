package com.iti.domain.settings.repository

import com.iti.domain.core.Result
import com.iti.domain.model.User
import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow


interface AppPreferencesRepository {

    val preferences: Flow<AppPreferences>

    suspend fun setThemeMode(mode: ThemeMode): Result<Unit>

    suspend fun setLanguage(language: AppLanguage): Result<Unit>

    suspend fun setRemindersEnabled(enabled: Boolean): Result<Unit>

    suspend fun setErrorSoundsEnabled(enabled: Boolean): Result<Unit>

    suspend fun setDataSaverEnabled(enabled: Boolean): Result<Unit>
    
    suspend fun saveUser(user: User): Result<Unit>
    
    suspend fun clearUser(): Result<Unit>
}
