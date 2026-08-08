package com.iti.presentation.circle.state

import androidx.annotation.StringRes
import com.iti.meeting.domain.model.circle.Circle

sealed interface CircleJoinUiState {
    data object Idle : CircleJoinUiState
    data object Joining : CircleJoinUiState
    data object Joined : CircleJoinUiState
    data class PendingApproval(val membershipId: String) : CircleJoinUiState
    data class Error(@StringRes val messageRes: Int) : CircleJoinUiState
}

data class CircleDetailsUiState(
    val circle: Circle? = null,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val passwordPromptVisible: Boolean = false,
    val password: String = "",
    val joinState: CircleJoinUiState = CircleJoinUiState.Idle,
    val isMember: Boolean = false,
    val isLeaveDialogVisible: Boolean = false,
    val isLeaving: Boolean = false,
)

sealed interface CircleDetailsIntent {
    data object Retry : CircleDetailsIntent
    data object JoinClicked : CircleDetailsIntent
    data object EnterClicked : CircleDetailsIntent
    data class PasswordChanged(val password: String) : CircleDetailsIntent
    data object SubmitJoin : CircleDetailsIntent
    data object DismissPasswordPrompt : CircleDetailsIntent
    data object LeaveClicked : CircleDetailsIntent
    data object ConfirmLeave : CircleDetailsIntent
    data object DismissLeaveDialog : CircleDetailsIntent
}

sealed interface CircleDetailsEffect {
    data object NavigateBack : CircleDetailsEffect
    data class OpenJoining(val circleId: String, val membershipId: String) : CircleDetailsEffect
    data class OpenSession(val circleId: String) : CircleDetailsEffect
    data class ShowMessage(@StringRes val messageRes: Int) : CircleDetailsEffect
}
