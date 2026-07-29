package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.getOrNull
import com.iti.domain.usecase.circle.CancelJoinCircleUseCase
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.presentation.circle.state.JoiningCircleEffect
import com.iti.presentation.circle.state.JoiningCircleIntent
import com.iti.presentation.circle.state.JoiningCircleUiState
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class JoiningCircleViewModel(
    private val circleId: String,
    private val getCircles: GetStudyCirclesUseCase,
    private val cancelJoin: CancelJoinCircleUseCase,
) : ViewModel(),
    StateHolder<JoiningCircleUiState> by DefaultStateHolder(JoiningCircleUiState()),
    EffectPublisher<JoiningCircleEffect> by DefaultEffectPublisher() {

    init {
        observeCircle()
        simulateApproval()
    }

    fun onIntent(intent: JoiningCircleIntent) = when (intent) {
        JoiningCircleIntent.CancelRequest -> cancel()
    }

    private fun observeCircle() {
        getCircles()
            .catch {}
            .onEach { result ->
                val circles = result.getOrNull() ?: return@onEach
                val circle = circles.firstOrNull { it.id == circleId }
                updateState { copy(circle = circle, isLoading = circle == null) }
            }
            .launchIn(viewModelScope)
    }

    private fun simulateApproval() {
        viewModelScope.launch {
            delay(APPROVAL_DELAY_MS)
            sendEffect(JoiningCircleEffect.NavigateToSession(circleId))
        }
    }

    private fun cancel() {
        viewModelScope.launch {
            cancelJoin(circleId)
            sendEffect(JoiningCircleEffect.NavigateBack)
        }
    }

    private companion object {
        const val APPROVAL_DELAY_MS = 4_000L
    }
}
