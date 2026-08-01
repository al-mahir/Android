package com.iti.meeting.presentation.call

sealed interface CallUiState {
    data object Connecting : CallUiState
    data class InCall(
        val remoteUid: Int?,
        val isMicEnabled: Boolean = false,
        val isCameraEnabled: Boolean = false,
        val isSpeakerEnabled: Boolean = true,
        // Assumed on until an explicit mute event says otherwise - Agora doesn't report remote
        // track state until the first onUserMuteAudio/onUserMuteVideo callback fires.
        val isRemoteMicEnabled: Boolean = true,
        val isRemoteCameraEnabled: Boolean = true,
        val callDurationSeconds: Long = 0L,
        val isReconnecting: Boolean = false,
    ) : CallUiState
    data object Ended : CallUiState
    data class Error(val message: String) : CallUiState
}
