package com.example.mushaf.data.recite.remote

import com.example.mushaf.data.recite.remote.dto.FeedbackEnvelopeDto
import com.example.mushaf.domain.model.recite.AudioFrame

/** What the client sends during a live session, after the handshake. */
sealed interface LiveSessionCommand {

    /** Gated audio to put on the wire, as a binary frame. */
    class Audio(val frame: AudioFrame) : LiveSessionCommand

    /**
     * The reciter jumped elsewhere — a page turn or an āyah tap. Without this the tracker keeps
     * searching near the old position and starts reporting mismatches that are not mistakes.
     */
    data class Seek(val sura: Int, val aya: Int, val wordIdx: Int = 0) : LiveSessionCommand

    /**
     * Stop reciting. The server flushes the in-progress utterance, which may produce one final
     * feedback event, then replies `done`. Emitting this and *then* completing the command flow
     * is what lets the last chunk of a recitation survive.
     */
    data object End : LiveSessionCommand
}

/** What the session reports back. */
sealed interface LiveSessionEvent {

    /**
     * The handshake completed.
     *
     * [engine] is what actually ran, which is not necessarily what was requested — an unbuilt
     * engine falls back silently and the ack is the only place that is visible. Compare it to
     * the request and tell the user on a mismatch.
     */
    data class Started(
        val sessionId: String,
        val engine: String,
        val sampleRate: Int,
        val requestedEngine: String?,
    ) : LiveSessionEvent {
        /** True when the server substituted a different engine than the one asked for. */
        val engineSubstituted: Boolean
            get() = requestedEngine != null && !requestedEngine.equals(engine, ignoreCase = true)
    }

    /** One finalized waqf chunk. Raw transport shape; step 4 maps this into domain models. */
    data class Feedback(val envelope: FeedbackEnvelopeDto) : LiveSessionEvent

    /** The server acknowledged `end` and is closing. Nothing further will arrive. */
    data object Done : LiveSessionEvent
}

/**
 * A session that ended in a way the caller has to react to.
 *
 * The service's close codes are not all equal: 1002 is a permanent protocol bug in the client,
 * while 1006 is usually the network and should be retried from the last cursor.
 */
class LiveSessionException(
    message: String,
    val closeCode: Short? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
