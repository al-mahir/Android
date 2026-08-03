package com.iti.sheikh.presentation.circle.state

import androidx.annotation.StringRes
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.PendingJoinRequest

data class SheikhCircleManageUiState(
    val circle: Circle? = null,
    val pendingRequests: List<PendingJoinRequest> = emptyList(),
    val members: List<CircleMember> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val actionInProgress: Boolean = false,
)

sealed interface SheikhCircleManageIntent {
    data object Retry : SheikhCircleManageIntent
    data class ApproveRequest(val userId: String) : SheikhCircleManageIntent
    data class RejectRequest(val userId: String) : SheikhCircleManageIntent
    data class RemoveMember(val userId: String) : SheikhCircleManageIntent
    data object StartClicked : SheikhCircleManageIntent
    data object EndClicked : SheikhCircleManageIntent
    data object CancelClicked : SheikhCircleManageIntent
}

sealed interface SheikhCircleManageEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : SheikhCircleManageEffect
}
