package com.iti.presentation.circle.state

import com.iti.meeting.domain.model.circle.Circle

data class InSessionUiState(
    val circle: Circle? = null,
    val isMicMuted: Boolean = true,
    val isHandRaised: Boolean = false,
    val participants: List<SessionParticipant> = emptyList(),
    val unreadChatCount: Int = 0,
    val speakingParticipantId: String? = null,
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
    data object OpenChat : InSessionIntent
    data object OpenMushaf : InSessionIntent
}

sealed interface InSessionEffect {
    data object NavigateBack : InSessionEffect
    data object OpenMushaf : InSessionEffect
}
