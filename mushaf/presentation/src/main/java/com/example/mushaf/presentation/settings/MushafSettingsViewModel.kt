package com.example.mushaf.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.presentation.core.mvi.DefaultStateHolder
import com.example.mushaf.presentation.core.mvi.StateHolder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class MushafSettingsUiState(
    val isTajweedEnabled: Boolean = true,
    val isLoading: Boolean = true,
)

sealed interface MushafSettingsIntent {
    data class TajweedToggled(val enabled: Boolean) : MushafSettingsIntent
}


class MushafSettingsViewModel(
    private val observeReaderPreferences: ObserveReaderPreferencesUseCase,
    private val setTajweedEnabled: SetTajweedEnabledUseCase,
) : ViewModel(),
    StateHolder<MushafSettingsUiState> by DefaultStateHolder(MushafSettingsUiState()) {

    init {
        observeReaderPreferences()
            .onEach { prefs ->
                updateState { copy(isTajweedEnabled = prefs.tajweedEnabled, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: MushafSettingsIntent) {
        when (intent) {
            is MushafSettingsIntent.TajweedToggled -> viewModelScope.launch {
                setTajweedEnabled(intent.enabled)
            }
        }
    }
}
