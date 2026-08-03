package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.model.circle.CircleJoinError
import com.iti.meeting.domain.model.circle.CircleJoinResult
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.presentation.R
import com.iti.presentation.circle.state.CircleDetailsEffect
import com.iti.presentation.circle.state.CircleDetailsIntent
import com.iti.presentation.circle.state.CircleDetailsUiState
import com.iti.presentation.circle.state.CircleJoinUiState
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.launch

class CircleDetailsViewModel(
    private val circleId: String,
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<CircleDetailsUiState> by DefaultStateHolder(CircleDetailsUiState()),
    EffectPublisher<CircleDetailsEffect> by DefaultEffectPublisher() {

    init {
        load()
    }

    fun onIntent(intent: CircleDetailsIntent) = when (intent) {
        CircleDetailsIntent.Retry -> load()
        CircleDetailsIntent.JoinClicked -> onJoinClicked()
        is CircleDetailsIntent.PasswordChanged -> updateState { copy(password = intent.password) }
        CircleDetailsIntent.SubmitJoin -> join(currentState.password)
        CircleDetailsIntent.DismissPasswordPrompt -> updateState { copy(passwordPromptVisible = false, password = "") }
    }

    private fun load() {
        updateState { copy(isLoading = true, isError = false) }
        viewModelScope.launch {
            circleRepository.getCircle(circleId).fold(
                onSuccess = { circle ->
                    updateState { copy(circle = circle, isLoading = false, isError = false) }
                },
                onFailure = { updateState { copy(isLoading = false, isError = true) } },
            )
        }
    }

    private fun onJoinClicked() {
        val circle = currentState.circle ?: return
        if (circle.requiresApproval) {
            updateState { copy(passwordPromptVisible = true) }
        } else {
            join(null)
        }
    }

    private fun join(password: String?) {
        if (currentState.joinState is CircleJoinUiState.Joining) return
        updateState { copy(joinState = CircleJoinUiState.Joining, passwordPromptVisible = false) }

        viewModelScope.launch {
            when (val result = circleRepository.joinCircle(circleId, password)) {
                is CircleJoinResult.Joined -> {
                    updateState { copy(joinState = CircleJoinUiState.Joined) }
                    sendEffect(CircleDetailsEffect.OpenSession(circleId))
                }

                is CircleJoinResult.PendingApproval -> {
                    updateState { copy(joinState = CircleJoinUiState.PendingApproval(result.membershipId)) }
                    sendEffect(CircleDetailsEffect.OpenJoining(circleId, result.membershipId))
                }

                is CircleJoinResult.Error -> {
                    val messageRes = when (result.error) {
                        CircleJoinError.CIRCLE_FULL -> R.string.circle_join_error_full
                        CircleJoinError.TIME_CONFLICT -> R.string.circle_join_error_conflict
                        CircleJoinError.INVALID_PASSWORD -> R.string.circle_join_error_password
                        CircleJoinError.UNKNOWN -> R.string.circle_join_error_generic
                    }
                    updateState {
                        copy(
                            joinState = CircleJoinUiState.Error(messageRes),
                            passwordPromptVisible = result.error == CircleJoinError.INVALID_PASSWORD,
                        )
                    }
                    sendEffect(CircleDetailsEffect.ShowMessage(messageRes))
                }
            }
        }
    }
}
