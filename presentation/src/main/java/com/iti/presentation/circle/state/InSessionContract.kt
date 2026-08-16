package com.iti.presentation.circle.state

import androidx.annotation.StringRes
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.PendingJoinRequest

data class InSessionUiState(
    val circle: Circle? = null,
    val isMicMuted: Boolean = true,
    val isHandRaised: Boolean = false,
    val participants: List<SessionParticipant> = emptyList(),
    /** Pending join requests — populated only for the host (isHost = true). */
    val pendingRequests: List<PendingJoinRequest> = emptyList(),
    val unreadChatCount: Int = 0,
    val speakingParticipantId: String? = null,
    val isLeaveDialogVisible: Boolean = false,
    val isLeaving: Boolean = false,
    val isHost: Boolean = false,
    /** True while an approve/reject request action is in flight. */
    val actionInProgress: Boolean = false,
)

data class SessionParticipant(
    val id: String,
    val name: String,
    val initials: String,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = true,
)

sealed interface InSessionIntent {
    data object ToggleMic : InSessionIntent
    data object ToggleRaiseHand : InSessionIntent
    data object Leave : InSessionIntent
    data object ConfirmLeave : InSessionIntent
    data object DismissLeaveDialog : InSessionIntent
    data object OpenChat : InSessionIntent
    data object OpenMushaf : InSessionIntent
    /** Host approves a pending join request. */
    data class ApproveRequest(val userId: String) : InSessionIntent
    /** Host rejects a pending join request. */
    data class RejectRequest(val userId: String) : InSessionIntent
}

sealed interface InSessionEffect {
    data object NavigateBack : InSessionEffect
    data object OpenMushaf : InSessionEffect
    data class ShowMessage(@StringRes val messageRes: Int) : InSessionEffect
}
