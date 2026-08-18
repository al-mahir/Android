package com.iti.sheikh.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.sheikh.presentation.circle.state.SheikhCircleListEffect
import com.iti.sheikh.presentation.circle.state.SheikhCircleListIntent
import com.iti.sheikh.presentation.circle.state.SheikhCircleListUiState
import com.iti.sheikh.presentation.core.mvi.DefaultEffectPublisher
import com.iti.sheikh.presentation.core.mvi.DefaultStateHolder
import com.iti.sheikh.presentation.core.mvi.EffectPublisher
import com.iti.sheikh.presentation.core.mvi.StateHolder
import kotlinx.coroutines.launch

class SheikhCircleListViewModel(
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<SheikhCircleListUiState> by DefaultStateHolder(SheikhCircleListUiState()),
    EffectPublisher<SheikhCircleListEffect> by DefaultEffectPublisher() {

    init {
        load()
    }

    fun onIntent(intent: SheikhCircleListIntent) = when (intent) {
        is SheikhCircleListIntent.CircleClicked ->
            sendEffect(SheikhCircleListEffect.OpenCircle(intent.circleId))
        SheikhCircleListIntent.CreateCircleClicked ->
            sendEffect(SheikhCircleListEffect.OpenCreateCircle)
        SheikhCircleListIntent.Retry -> load()
    }

    private fun load() {
        updateState { copy(isLoading = true, isError = false) }
        viewModelScope.launch {
            circleRepository.getMyCircles().fold(
                onSuccess = { circles ->
                    updateState { copy(circles = circles, isLoading = false, isError = false) }
                },
                onFailure = { updateState { copy(isLoading = false, isError = true) } },
            )
        }
    }
}
