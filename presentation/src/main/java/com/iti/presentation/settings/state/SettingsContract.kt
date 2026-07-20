package com.iti.presentation.settings.state

import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.settings.model.ThemeMode

enum class SettingsSheet {
    LANGUAGE,
    THEME,
}

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val isLoading: Boolean = true,
    val visibleSheet: SettingsSheet? = null,
    val isDeleteRecordingsDialogVisible: Boolean = false,
    val isDeletingRecordings: Boolean = false,
    val appVersion: String = "",
) {
    val themeMode: ThemeMode get() = preferences.themeMode
    val language: AppLanguage get() = preferences.language
}

sealed interface SettingsIntent {
    data object LanguageClicked : SettingsIntent
    data object ThemeClicked : SettingsIntent
    data object SheetDismissed : SettingsIntent

    data class LanguageSelected(val language: AppLanguage) : SettingsIntent
    data class ThemeSelected(val mode: ThemeMode) : SettingsIntent

    data class RemindersToggled(val enabled: Boolean) : SettingsIntent
    data class ErrorSoundsToggled(val enabled: Boolean) : SettingsIntent
    data class DataSaverToggled(val enabled: Boolean) : SettingsIntent

    data object DeleteRecordingsClicked : SettingsIntent
    data object DeleteRecordingsConfirmed : SettingsIntent
    data object DeleteRecordingsDismissed : SettingsIntent
}

sealed interface SettingsEffect {
    data class ShowMessage(val messageRes: Int) : SettingsEffect
}
