package com.iti.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.ThemeMode
import com.iti.domain.usecase.settings.DeleteAllRecordingsUseCase
import com.iti.domain.usecase.settings.ObserveAppPreferencesUseCase
import com.iti.domain.usecase.settings.SetAppLanguageUseCase
import com.iti.domain.usecase.settings.SetDataSaverEnabledUseCase
import com.iti.domain.usecase.settings.SetErrorSoundsEnabledUseCase
import com.iti.domain.usecase.settings.SetRemindersEnabledUseCase
import com.iti.domain.usecase.settings.SetThemeModeUseCase
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.settings.state.SettingsEffect
import com.iti.presentation.settings.state.SettingsIntent
import com.iti.presentation.settings.state.SettingsSheet
import com.iti.presentation.settings.state.SettingsUiState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class SettingsViewModel(
    private val appVersion: String,
    private val observePreferences: ObserveAppPreferencesUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setLanguage: SetAppLanguageUseCase,
    private val setRemindersEnabled: SetRemindersEnabledUseCase,
    private val setErrorSoundsEnabled: SetErrorSoundsEnabledUseCase,
    private val setDataSaverEnabled: SetDataSaverEnabledUseCase,
    private val deleteAllRecordings: DeleteAllRecordingsUseCase,
) : ViewModel(),
    StateHolder<SettingsUiState> by DefaultStateHolder(SettingsUiState()),
    EffectPublisher<SettingsEffect> by DefaultEffectPublisher() {

    init {
        updateState { copy(appVersion = this@SettingsViewModel.appVersion) }
        observe()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.LanguageClicked -> showSheet(SettingsSheet.LANGUAGE)
            SettingsIntent.ThemeClicked -> showSheet(SettingsSheet.THEME)
            SettingsIntent.SheetDismissed -> updateState { copy(visibleSheet = null) }

            is SettingsIntent.LanguageSelected -> selectLanguage(intent.language)
            is SettingsIntent.ThemeSelected -> selectTheme(intent.mode)

            is SettingsIntent.RemindersToggled ->
                viewModelScope.launch { setRemindersEnabled(intent.enabled) }

            is SettingsIntent.ErrorSoundsToggled ->
                viewModelScope.launch { setErrorSoundsEnabled(intent.enabled) }

            is SettingsIntent.DataSaverToggled ->
                viewModelScope.launch { setDataSaverEnabled(intent.enabled) }

            SettingsIntent.DeleteRecordingsClicked ->
                updateState { copy(isDeleteRecordingsDialogVisible = true) }

            SettingsIntent.DeleteRecordingsDismissed ->
                updateState { copy(isDeleteRecordingsDialogVisible = false) }

            SettingsIntent.DeleteRecordingsConfirmed -> deleteRecordings()
        }
    }

    private fun showSheet(sheet: SettingsSheet) {
        updateState { copy(visibleSheet = sheet) }
    }

    private fun selectLanguage(language: AppLanguage) {
        updateState { copy(visibleSheet = null) }
        viewModelScope.launch { setLanguage(language) }
    }

    private fun selectTheme(mode: ThemeMode) {
        updateState { copy(visibleSheet = null) }
        viewModelScope.launch { setThemeMode(mode) }
    }

    private fun deleteRecordings() {
        updateState { copy(isDeletingRecordings = true) }
        viewModelScope.launch {
            runCatching { deleteAllRecordings() }
                .onSuccess {
                    sendEffect(SettingsEffect.ShowMessage(R.string.settings_recordings_deleted))
                }
                .onFailure {
                    sendEffect(SettingsEffect.ShowMessage(R.string.settings_recordings_delete_failed))
                }

            updateState {
                copy(isDeletingRecordings = false, isDeleteRecordingsDialogVisible = false)
            }
        }
    }

    private fun observe() {
        observePreferences()
            .onEach { preferences ->
                updateState { copy(preferences = preferences, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }
}
