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
    /** True while fetching the Agora token to enter the live session. */
    val isTokenLoading: Boolean = false,
    /** Edit-circle dialog visibility */
    val isEditDialogVisible: Boolean = false,
    val editName: String = "",
    val editStartDate: String = "",
    val editEndDate: String = "",
)

sealed interface SheikhCircleManageIntent {
    data object Retry : SheikhCircleManageIntent
    data class ApproveRequest(val userId: String) : SheikhCircleManageIntent
    data class RejectRequest(val userId: String) : SheikhCircleManageIntent
    data class RemoveMember(val userId: String) : SheikhCircleManageIntent
    data object StartClicked : SheikhCircleManageIntent
    data object EndClicked : SheikhCircleManageIntent
    data object CancelClicked : SheikhCircleManageIntent
    /** Fetch the Agora token and enter the ongoing circle session. Used for recovery
     *  when the sheikh navigated away or the token fetch failed during [StartClicked]. */
    data object JoinSessionClicked : SheikhCircleManageIntent
    /** Opens the edit dialog pre-populated with current circle data. */
    data object EditClicked : SheikhCircleManageIntent
    data class EditNameChanged(val name: String) : SheikhCircleManageIntent
    data class EditStartDateChanged(val date: String) : SheikhCircleManageIntent
    data class EditEndDateChanged(val date: String) : SheikhCircleManageIntent
    data object SubmitEdit : SheikhCircleManageIntent
    data object DismissEdit : SheikhCircleManageIntent
}

sealed interface SheikhCircleManageEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : SheikhCircleManageEffect
    /** Shown after creating / loading a PRIVATE circle that has an invite token. */
    data class ShowInviteToken(val token: String, val circleName: String) : SheikhCircleManageEffect
    /** Navigate to the Agora video call screen after the circle is started (or rejoined). */
    data class OpenCall(
        val requestId: String,
        val token: String,
        val channelName: String,
        val userAccount: String,
    ) : SheikhCircleManageEffect
}
