package com.example.mushaf.domain.model.recite

/** What the reciter's session reports as it runs. */
sealed interface LiveRecitationEvent {

    /**
     * The session is live.
     *
     * [engine] is what actually ran. Requesting an engine the server did not build is not an
     * error — it substitutes its default silently — so [engineSubstituted] is the only signal
     * that grading is not what the reciter chose, and it must reach them.
     */
    data class Started(
        val sessionId: String,
        val engine: String,
        val requestedEngine: String?,
    ) : LiveRecitationEvent {
        val engineSubstituted: Boolean
            get() = requestedEngine != null && !requestedEngine.equals(engine, ignoreCase = true)
    }

    /**
     * Live microphone loudness, 0f..1f, and whether the speech gate is passing audio.
     *
     * Diagnostic and for the level meter only. A session where nothing is being said produces no
     * graded chunks at all, so this is the only evidence the pipeline is alive.
     */
    data class Level(val amplitude: Float, val isSpeaking: Boolean) : LiveRecitationEvent

    /** One graded chunk, roughly one per waqf. */
    data class Graded(val chunk: RecitationChunk) : LiveRecitationEvent

    /** The server flushed, acknowledged the end, and closed. Nothing further will arrive. */
    data object Finished : LiveRecitationEvent
}

/** What the reciter's actions ask of a running session. */
sealed interface RecitationControl {

    /**
     * The reciter jumped elsewhere — a page turn or an āyah tap.
     *
     * Without it the tracker keeps searching near the old position and starts reporting
     * mismatches that are not mistakes.
     */
    data class Seek(val position: RecitationCursor) : RecitationControl

    /**
     * Stop reciting.
     *
     * Releases the microphone, then tells the server to flush — which may still produce one last
     * graded chunk. The session ends on [LiveRecitationEvent.Finished], not on this.
     */
    data object Finish : RecitationControl
}
