package com.iti.data.settings.repository

import com.iti.data.settings.local.AppPreferencesDataStore
import com.iti.domain.core.Result
import com.iti.domain.core.resultOf
import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.settings.model.ThemeMode
import com.iti.domain.settings.repository.AppPreferencesRepository
import com.iti.domain.settings.repository.RecordingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val dataStore: AppPreferencesDataStore,
) : AppPreferencesRepository, RecordingsRepository {

    // ── AppPreferencesRepository ─────────────────────────────────────────

    override val preferences: Flow<AppPreferences> = dataStore.preferences

    override suspend fun setThemeMode(mode: ThemeMode): Result<Unit> =
        resultOf { dataStore.setThemeMode(mode) }

    override suspend fun setLanguage(language: AppLanguage): Result<Unit> =
        resultOf { dataStore.setLanguage(language) }

    override suspend fun setRemindersEnabled(enabled: Boolean): Result<Unit> =
        resultOf { dataStore.setRemindersEnabled(enabled) }

    override suspend fun setErrorSoundsEnabled(enabled: Boolean): Result<Unit> =
        resultOf { dataStore.setErrorSoundsEnabled(enabled) }

    override suspend fun setDataSaverEnabled(enabled: Boolean): Result<Unit> =
        resultOf { dataStore.setDataSaverEnabled(enabled) }

    // ── RecordingsRepository ──────────────────────────────────────────────
    // Fake pending a real recordings backend.

    override suspend fun deleteAll(): Result<Unit> = resultOf {
        delay(FAKE_DELETE_MILLIS)
    }

    private companion object {
        const val FAKE_DELETE_MILLIS = 900L
    }
}
