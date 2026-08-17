package com.iti.meeting.presentation.circle

import com.iti.meeting.presentation.call.audio.AudioOutputDevice
import kotlinx.coroutines.flow.StateFlow

/**
 * The circle audio transport, as its callers see it.
 *
 * An interface rather than the concrete [CircleAudioSessionController] so screen-level ViewModels
 * stay testable on the JVM — the controller creates an Agora engine and reads `AudioManager`, which
 * a plain unit test can neither do nor meaningfully stub.
 *
 * Deliberately takes no `Context`: the controller is constructed with the application context, and
 * threading an Activity context through the call sites only invited a leak and made this
 * unfakeable.
 */
interface CircleAudioSession {

    val state: StateFlow<CircleAudioSessionState>

    val currentState: CircleAudioSessionState get() = state.value

    /**
     * Joins [circleId]'s audio channel, fetching its own Agora credentials. Idempotent for a circle
     * already joined, so returning to the session screen doesn't restart a live session.
     *
     * @param circleTitle shown on the ongoing-session notification.
     */
    fun join(circleId: String, circleTitle: String)

    /** @return the new mic state. */
    fun toggleMic(): Boolean

    fun selectAudioDevice(device: AudioOutputDevice)

    /** Leaves the Agora channel. The REST leave/end call is the caller's concern. */
    fun leave()
}
