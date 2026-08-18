package com.iti.presentation.circle.state

import androidx.annotation.StringRes
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.PendingJoinRequest
import com.iti.meeting.presentation.call.audio.AudioOutputDevice
import com.iti.meeting.presentation.circle.CircleAudioStatus

data class InSessionUiState(
    val circle: Circle? = null,
    /** Mirrors the real Agora publish state, not just a UI toggle. Members join muted. */
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
    /** Live audio-transport state for the circle. */
    val audioStatus: CircleAudioStatus = CircleAudioStatus.Idle,
    val audioDevice: AudioOutputDevice = AudioOutputDevice.SPEAKER,
    val availableAudioDevices: List<AudioOutputDevice> = listOf(AudioOutputDevice.SPEAKER),
    val isAudioOutputSheetVisible: Boolean = false,
    /** True once the mic permission has been denied, so the UI can explain why nobody can hear them. */
    val isMicPermissionDenied: Boolean = false,
) {
    /** The mic control only means anything once audio is actually flowing. */
    val isMicControlEnabled: Boolean get() = audioStatus is CircleAudioStatus.Live
}

data class SessionParticipant(
    val id: String,
    val name: String,
    val initials: String,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = true,
    /** False until this member's Agora stream is seen — i.e. they're on the roster but not
     * actually connected to the audio channel yet. */
    val isConnected: Boolean = false,
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

    /** Mic permission result from the screen — audio can only start once it's granted. */
    data class MicPermissionResult(val granted: Boolean) : InSessionIntent
    data object OpenAudioOutputPicker : InSessionIntent
    data object DismissAudioOutputPicker : InSessionIntent
    data class SelectAudioDevice(val device: AudioOutputDevice) : InSessionIntent
    /** Retry after a failed join, or after the host starts a circle that wasn't ongoing yet. */
    data object RetryAudio : InSessionIntent
}

sealed interface InSessionEffect {
    data object NavigateBack : InSessionEffect
    data object OpenMushaf : InSessionEffect
    data class ShowMessage(@StringRes val messageRes: Int) : InSessionEffect
    /** Ask the host screen to request RECORD_AUDIO (and POST_NOTIFICATIONS on 33+). */
    data object RequestMicPermission : InSessionEffect
}
