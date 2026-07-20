package com.iti.domain.usecase.settings

import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.settings.model.ThemeMode
import com.iti.domain.settings.repository.AppPreferencesRepository
import com.iti.domain.settings.repository.RecordingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveAppPreferencesUseCase(
    private val repository: AppPreferencesRepository,
) {
    operator fun invoke(): Flow<AppPreferences> = repository.preferences
}

class SetThemeModeUseCase(
    private val repository: AppPreferencesRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class SetAppLanguageUseCase(
    private val repository: AppPreferencesRepository,
) {
    suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}

class SetRemindersEnabledUseCase(
    private val repository: AppPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setRemindersEnabled(enabled)
}

class SetErrorSoundsEnabledUseCase(
    private val repository: AppPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setErrorSoundsEnabled(enabled)
}

class SetDataSaverEnabledUseCase(
    private val repository: AppPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setDataSaverEnabled(enabled)
}

/** Irreversible. The caller is responsible for confirming with the user first. */
class DeleteAllRecordingsUseCase(
    private val repository: RecordingsRepository,
) {
    suspend operator fun invoke() = repository.deleteAll()
}
