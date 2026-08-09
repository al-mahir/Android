package com.iti.meeting.presentation.call.state

sealed interface CallUiState {
    data object Idle : CallUiState
    data object Connecting : CallUiState
    data class InCall(
        val remoteUid: Int?,
        val isMicEnabled: Boolean = false,
        val isCameraEnabled: Boolean = false,
        val isSpeakerEnabled: Boolean = true,
        val isRemoteMicEnabled: Boolean = true,
        val isRemoteCameraEnabled: Boolean = true,
        val callDurationSeconds: Long = 0L,
        val isReconnecting: Boolean = false,
    ) : CallUiState

    data object Ended : CallUiState
    data class Error(val message: String) : CallUiState
}
