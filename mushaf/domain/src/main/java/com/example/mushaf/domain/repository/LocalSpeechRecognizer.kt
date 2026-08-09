package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.local.LocalTranscript
import kotlinx.coroutines.flow.SharedFlow

/**
 * A streaming local recognizer - deliberately shaped as "fed frames", not "owns its own mic
 * session". `android.speech.SpeechRecognizer` used to fill this role via its own discrete
 * start/stop sessions, which meant a second concurrent mic session and OS chimes on every restart
 * (see docs/features/06-taahud-speechrecognizer-status.md). This contract is fed from whatever
 * capture session is already open for the grading socket - one mic session, ever, for the whole
 * feature.
 */
interface LocalSpeechRecognizer {

    /** True once the underlying model is ready to decode - see `AsrModelRepository`. Callers
     * should still call [accept] opportunistically either way; it's a no-op until this is true. */
    val isAvailable: Boolean

    /**
     * The running phoneme transcript, re-emitted every time a decode extends it.
     *
     * Not words: the model's vocabulary has no space and no word-boundary symbol, so there is no
     * point at which it can hand over "a word" - see [LocalTranscript] and
     * [com.example.mushaf.domain.model.recite.local.PhonemeCursorTracker], which does the
     * segmenting against the expected text instead.
     */
    val transcript: SharedFlow<LocalTranscript>

    /** Feeds one frame of 16kHz mono PCM16 audio. Safe to call every frame, unconditionally -
     * never throws or blocks the caller's session if the model isn't ready yet. */
    suspend fun accept(frame: AudioFrame)

    /** Clears in-progress decode state - call at the start/end of a live session so a new
     * session never inherits a stale partial utterance from the previous one. */
    suspend fun reset()
}
