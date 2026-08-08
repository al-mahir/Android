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
        checkMembership()
    }

    fun onIntent(intent: CircleDetailsIntent) = when (intent) {
        CircleDetailsIntent.Retry -> load()
        CircleDetailsIntent.JoinClicked -> onJoinClicked()
        CircleDetailsIntent.EnterClicked -> sendEffect(CircleDetailsEffect.OpenSession(circleId))
        is CircleDetailsIntent.PasswordChanged -> updateState { copy(password = intent.password) }
        CircleDetailsIntent.SubmitJoin -> join(currentState.password)
        CircleDetailsIntent.DismissPasswordPrompt -> updateState { copy(passwordPromptVisible = false, password = "") }
        CircleDetailsIntent.LeaveClicked -> updateState { copy(isLeaveDialogVisible = true) }
        CircleDetailsIntent.ConfirmLeave -> leaveCircle()
        CircleDetailsIntent.DismissLeaveDialog -> updateState { copy(isLeaveDialogVisible = false) }
    }

    private fun leaveCircle() {
        if (currentState.isLeaving) return
        updateState { copy(isLeaving = true) }
        viewModelScope.launch {
            circleRepository.leaveCircle(circleId).fold(
                onSuccess = {
                    updateState { copy(isLeaving = false, isLeaveDialogVisible = false, isMember = false) }
                    sendEffect(CircleDetailsEffect.ShowMessage(R.string.circle_leave_success))
                },
                onFailure = {
                    updateState { copy(isLeaving = false, isLeaveDialogVisible = false) }
                    sendEffect(CircleDetailsEffect.ShowMessage(R.string.circle_leave_error))
                },
            )
        }
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

    /** Marks the screen as already-joined so it offers "Enter Circle" instead of "Join".
     * A membership-check failure leaves [CircleDetailsUiState.isMember] false. */
    private fun checkMembership() {
        viewModelScope.launch {
            circleRepository.getMyCircles().fold(
                onSuccess = { circles -> updateState { copy(isMember = circles.any { it.id == circleId }) } },
                onFailure = { /* Optional; the join call below still guards against duplicates. */ },
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
                    if (result.error == CircleJoinError.ALREADY_MEMBER) {
                        updateState { copy(joinState = CircleJoinUiState.Idle, isMember = true, passwordPromptVisible = false) }
                        sendEffect(CircleDetailsEffect.ShowMessage(R.string.circle_join_error_already_member))
                        return@launch
                    }
                    val messageRes = when (result.error) {
                        CircleJoinError.CIRCLE_FULL -> R.string.circle_join_error_full
                        CircleJoinError.TIME_CONFLICT -> R.string.circle_join_error_conflict
                        CircleJoinError.INVALID_PASSWORD -> R.string.circle_join_error_password
                        CircleJoinError.ALREADY_MEMBER -> R.string.circle_join_error_already_member
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
