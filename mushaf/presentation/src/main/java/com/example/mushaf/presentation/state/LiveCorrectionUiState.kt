package com.example.mushaf.presentation.state

import com.example.mushaf.domain.model.recite.NonVerseSegment
import com.example.mushaf.domain.model.recite.RecitationCandidate
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.RecitationWordMark

/**
 * What the service concluded about the most recent chunk.
 *
 * Kept distinct from "no feedback yet": [NO_MATCH] is the service actively declining to place
 * what it heard, and the reciter is owed a neutral "didn't catch that" rather than silence.
 */
enum class ChunkOutcome {
    /** The passage was identified and graded. */
    GRADED,

    /** It occurs in several places and could not be disambiguated. Show candidates, mark nothing. */
    AMBIGUOUS,

    /** Nothing matched — noise, a non-Qur'anic utterance, or too short to place. Mark nothing. */
    NO_MATCH,
}

/**
 * The live AI correction session, as the reader screen sees it.
 *
 * A presentation-only aggregate: it combines what several domain streams report into the shape
 * one screen renders, and is not a domain model (AGENTS.md §3b).
 */
data class LiveCorrectionUiState(
    /** Connecting, or reconnecting after a drop. The microphone is not open yet. */
    val isConnecting: Boolean = false,

    /** The handshake completed and the session is live. */
    val isActive: Boolean = false,

    /** The engine that actually ran — not necessarily the one requested. */
    val engine: String? = null,

    /**
     * The server substituted a different engine than the one asked for.
     *
     * Must reach the reciter: on `zipformer` a session gives word tracking and **no** tajwīd
     * grading, so silently swapping it changes what the app is able to teach.
     */
    val engineSubstituted: Boolean = false,

    /** Accumulated per-word verdicts, keyed by `MushafWord.id`. */
    val wordFeedback: Map<String, RecitationWordFeedback> = emptyMap(),

    /** Populated only when the last chunk was ambiguous. Show these; mark no word. */
    val candidates: List<RecitationCandidate> = emptyList(),

    /** Recognised istiʿādha / basmalah / ṣadaqa — acknowledge, never score. */
    val nonVerse: List<NonVerseSegment> = emptyList(),

    val lastOutcome: ChunkOutcome? = null,

    /** Where the reciter is. The resume point if the connection drops. */
    val cursor: RecitationCursor? = null,

    /** Word whose mistake detail is open, if any. */
    val selectedMistakeWordId: String? = null,
) {
    /**
     * Words that count as mistakes.
     *
     * Derived rather than tallied, so hints and unverified words can never drift into the count:
     * `almost` means the model was not confident enough to accuse, and counting it would undo
     * the softening the service applied on purpose.
     */
    val mistakeWords: List<RecitationWordFeedback>
        get() = wordFeedback.values.filter { it.countsAsMistake }

    val mistakeCount: Int get() = mistakeWords.size

    /**
     * Words the service actually scored.
     *
     * Unverified words are excluded: they sat on a chunk boundary and were never checked, so
     * counting them either way would invent a result the model never produced.
     */
    val scoredWordCount: Int
        get() = wordFeedback.values.count { it.mark != RecitationWordMark.UNVERIFIED }

    /**
     * Share of scored words with no confident mistake, or null before anything has been scored.
     *
     * Hints count as *not* mistakes, matching the service's own softening — a finding it was not
     * confident enough to assert must not be re-hardened into a lower score here.
     *
     * The thresholds behind that distinction are documented as uncalibrated, so this is session
     * feedback and not an assessment. Do not build a grade, streak or ranking on it.
     */
    val accuracy: Float?
        get() = scoredWordCount
            .takeIf { it > 0 }
            ?.let { (it - mistakeCount).toFloat() / it }

    /** Feedback for a word, or null if it has not been graded in this session. */
    fun feedbackFor(wordId: String): RecitationWordFeedback? = wordFeedback[wordId]

    val selectedMistake: RecitationWordFeedback?
        get() = selectedMistakeWordId?.let { wordFeedback[it] }?.takeIf { it.countsAsMistake }
}
