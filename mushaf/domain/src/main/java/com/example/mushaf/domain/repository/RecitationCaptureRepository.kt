package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.SpeechEvent
import com.example.mushaf.domain.model.recite.SpeechGateConfig
import kotlinx.coroutines.flow.Flow

/**
 * Access to the reciter's voice as a stream of PCM frames.
 *
 * Capability-oriented: one member for "listen to the microphone". Later stages of the live
 * correction pipeline (speech gating, the AI session socket) are separate capabilities and do
 * not belong here.
 */
interface RecitationCaptureRepository {

    /**
     * Cold flow of microphone frames. Capture starts when collection starts and the microphone
     * is released when collection stops or fails, so cancelling the collecting coroutine is the
     * only stop signal needed.
     *
     * Throws [SecurityException] if `RECORD_AUDIO` was not granted, and [IllegalStateException]
     * if the device refuses to open a recorder in the required format.
     */
    fun capture(): Flow<AudioFrame>

    /**
     * The same microphone, with the long silences between phrases removed — what a live
     * recitation session should actually put on the wire.
     *
     * A separate capability from [capture] rather than a flag on it: "hear the microphone" and
     * "hear the reciter" are different questions, and only the second one is allowed to discard
     * audio. Pass [SpeechGateConfig.Disabled] to stream continuously.
     *
     * Fails the same way [capture] does.
     */
    fun captureSpeech(config: SpeechGateConfig = SpeechGateConfig()): Flow<SpeechEvent>
}
