package com.iti.presentation.meetingrequest.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.presentation.meetingrequest.browse.SheikhBrowseEffect
import com.iti.presentation.meetingrequest.browse.SheikhBrowseIntent
import com.iti.presentation.meetingrequest.browse.SheikhBrowseUiState
import kotlinx.coroutines.launch

class SheikhBrowseViewModel(
    private val repository: MeetingRepository,
) : ViewModel(),
    StateHolder<SheikhBrowseUiState> by DefaultStateHolder(SheikhBrowseUiState()),
    EffectPublisher<SheikhBrowseEffect> by DefaultEffectPublisher() {

    init {
        load()
    }

    fun onIntent(intent: SheikhBrowseIntent) {
        when (intent) {
            SheikhBrowseIntent.Load, SheikhBrowseIntent.Refresh -> load()
            is SheikhBrowseIntent.SheikhSelected ->
                sendEffect(SheikhBrowseEffect.NavigateToRequest(intent.sheikhId))
        }
    }

    private fun load() = viewModelScope.launch {
        updateState { copy(isLoading = true, errorMessage = null) }
        repository.getAvailableSheikhs()
            .onSuccess { sheikhs -> updateState { copy(isLoading = false, sheikhs = sheikhs) } }
            .onFailure { error -> updateState { copy(isLoading = false, errorMessage = error.message) } }
    }
}






