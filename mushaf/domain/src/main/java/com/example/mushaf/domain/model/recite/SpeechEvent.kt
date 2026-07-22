package com.example.mushaf.domain.model.recite

/**
 * What the speech gate produced from the microphone.
 *
 * Frames are only reported once the gate has decided they are worth sending, which is why this
 * is an event stream rather than a plain frame stream: the gate opens *retroactively*, replaying
 * buffered pre-roll, so a frame's fate is not known at the moment it is captured.
 */
sealed interface SpeechEvent {

    /** Audio to put on the wire, in capture order. */
    class Audio(val frame: AudioFrame) : SpeechEvent

    /**
     * The gate closed: the reciter stopped and the trailing silence has been sent.
     *
     * This is the client's view of a waqf. The server reaches the same conclusion independently
     * from the silence it was just sent — this event does not replace that, and nothing is
     * signalled to the server here. It exists so the UI can settle (drop the level meter) and so
     * later steps can correlate a pause with the feedback event it produces.
     */
    data object SpeechEnded : SpeechEvent
}
