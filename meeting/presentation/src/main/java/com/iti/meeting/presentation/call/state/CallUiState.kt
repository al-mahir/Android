package com.iti.meeting.presentation.call.state

import com.iti.meeting.presentation.call.audio.AudioOutputDevice

sealed interface CallUiState {
    data object Idle : CallUiState
    data object Connecting : CallUiState
    data class InCall(
        val remoteUid: Int?,
        val isMicEnabled: Boolean = false,
        val isCameraEnabled: Boolean = false,
        val audioDevice: AudioOutputDevice = AudioOutputDevice.SPEAKER,
        val availableAudioDevices: List<AudioOutputDevice> = listOf(AudioOutputDevice.SPEAKER),
        val isFrontCamera: Boolean = true,
        val isTorchAvailable: Boolean = false,
        val isTorchOn: Boolean = false,
        val isRemoteMicEnabled: Boolean = true,
        val isRemoteCameraEnabled: Boolean = true,
        val callDurationSeconds: Long = 0L,
        val isReconnecting: Boolean = false,
    ) : CallUiState

    data object Ended : CallUiState

    /**
     * Carries a [reason] rather than a message string so the UI can localize it — the raw-`String`
     * version this replaced was the one thing in the call flow that could only ever render English.
     * [agoraErrorCode] is diagnostic only; it's appended to the message for support, not translated.
     */
    data class Error(val reason: CallErrorReason, val agoraErrorCode: Int? = null) : CallUiState
}

enum class CallErrorReason {
    /** The join never completed — timed out waiting for `onJoinChannelSuccess`. */
    CONNECT_FAILED,

    /** Was connected, then Agora reported the connection permanently failed. */
    CONNECTION_LOST,

    /** Agora surfaced an error code we don't map to anything friendlier. */
    ENGINE_ERROR,
}
