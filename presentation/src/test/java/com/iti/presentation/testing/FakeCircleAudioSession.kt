package com.iti.presentation.testing

import com.iti.meeting.presentation.call.audio.AudioOutputDevice
import com.iti.meeting.presentation.circle.CircleAudioSession
import com.iti.meeting.presentation.circle.CircleAudioSessionState
import com.iti.meeting.presentation.circle.CircleAudioStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [CircleAudioSession] for ViewModel tests — records the calls made to it and lets a test
 * drive the session state by hand, standing in for the real Agora callbacks.
 */
class FakeCircleAudioSession(
    initialState: CircleAudioSessionState = CircleAudioSessionState(),
) : CircleAudioSession {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<CircleAudioSessionState> = _state.asStateFlow()

    var joinedCircleId: String? = null
        private set
    var joinCount: Int = 0
        private set
    var leaveCount: Int = 0
        private set
    var selectedDevice: AudioOutputDevice? = null
        private set

    override fun join(circleId: String, circleTitle: String) {
        joinedCircleId = circleId
        joinCount++
        _state.value = _state.value.copy(circleId = circleId, status = CircleAudioStatus.Connecting)
    }

    override fun toggleMic(): Boolean {
        val enabled = !_state.value.isMicEnabled
        _state.value = _state.value.copy(isMicEnabled = enabled)
        return enabled
    }

    override fun selectAudioDevice(device: AudioOutputDevice) {
        selectedDevice = device
        _state.value = _state.value.copy(audioDevice = device)
    }

    override fun leave() {
        leaveCount++
        _state.value = CircleAudioSessionState()
    }

    /** Drives the session as the real controller would once Agora reports the channel joined. */
    fun emit(state: CircleAudioSessionState) {
        _state.value = state
    }
}
