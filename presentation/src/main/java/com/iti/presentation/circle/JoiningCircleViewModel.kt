package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.meeting.domain.repository.JoinRequestEvent
import com.iti.presentation.R
import com.iti.presentation.circle.state.JoiningCircleEffect
import com.iti.presentation.circle.state.JoiningCircleIntent
import com.iti.presentation.circle.state.JoiningCircleUiState
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class JoiningCircleViewModel(
    private val circleId: String,
    private val membershipId: String,
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<JoiningCircleUiState> by DefaultStateHolder(JoiningCircleUiState()),
    EffectPublisher<JoiningCircleEffect> by DefaultEffectPublisher() {

    init {
        observeCircle()
        observeApproval()
    }

    fun onIntent(intent: JoiningCircleIntent) = when (intent) {
        JoiningCircleIntent.CancelRequest -> cancel()
    }

    private fun observeCircle() {
        viewModelScope.launch {
            circleRepository.getCircle(circleId).fold(
                onSuccess = { circle -> updateState { copy(circle = circle, isLoading = false) } },
                onFailure = { updateState { copy(isLoading = false) } },
            )
        }
    }

    private fun observeApproval() {
        circleRepository.observeJoinRequestEvents(membershipId)
            .onEach { event ->
                when (event) {
                    is JoinRequestEvent.Approved -> sendEffect(JoiningCircleEffect.NavigateToSession(circleId))
                    is JoinRequestEvent.Rejected -> {
                        sendEffect(JoiningCircleEffect.ShowMessage(R.string.joining_circle_rejected))
                        sendEffect(JoiningCircleEffect.NavigateBack)
                    }
                }
            }
            .catch {
                sendEffect(JoiningCircleEffect.ShowMessage(R.string.joining_circle_wait_failed))
                sendEffect(JoiningCircleEffect.NavigateBack)
            }
            .launchIn(viewModelScope)
    }

    private fun cancel() {
        viewModelScope.launch {
            circleRepository.cancelJoinRequest(circleId)
            sendEffect(JoiningCircleEffect.NavigateBack)
        }
    }
}
