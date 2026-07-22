package com.example.mushaf.domain.model.recite

/** The service's raw verdict on a word. Read [RecitationWordFeedback.mark] instead of this. */
enum class RecitationWordStatus {
    CORRECT,
    /** The model was not confident enough to accuse. */
    ALMOST,
    ERROR,
}

/**
 * How a word should actually be rendered, with `trimmed` already folded in.
 *
 * This exists because the three service statuses cannot express "not checked", and a client that
 * renders that gap as an assertion undoes the safety property the service is built around. The
 * safe reading is therefore the *default* reading: [RecitationWordFeedback.mark] is the obvious
 * property to reach for, and it is impossible to get wrong.
 */
enum class RecitationWordMark {
    /** Verified correct. Safe to show a tick. */
    CORRECT,

    /**
     * A hint, never a mistake.
     *
     * Never list it among mistakes, never count it against a score. The service already softened
     * this finding on purpose; falsely correcting a perfect recitation is the one failure it must
     * not produce, and re-hardening it here would undo that.
     */
    HINT,

    /** A confident mistake. Correction UI belongs here and only here. */
    MISTAKE,

    /**
     * Not scored: the word sat on a chunk boundary and was cut by the chunker, not the reciter.
     *
     * Render neutrally — never a tick, never a green highlight. It carries no information about
     * the recitation, whatever the underlying [RecitationWordFeedback.status] says.
     */
    UNVERIFIED,
}

/**
 * Per-word feedback for one word of a graded chunk.
 *
 * @param isTrimmed the word was not scored. Overrides [status] for every rendering decision.
 */
data class RecitationWordFeedback(
    val position: RecitationCursor,
    val uthmani: String,
    val status: RecitationWordStatus,
    val mistakes: List<RecitationMistake>,
    val isTrimmed: Boolean,
) {
    /** Joins to `MushafWord.id`, so feedback lands on the right glyphs on the page. */
    val wordId: String get() = position.wordId

    /**
     * The renderable verdict. Trimming wins over [status]: an unscored word is unverified even
     * when the service labelled it `correct`.
     */
    val mark: RecitationWordMark
        get() = when {
            isTrimmed -> RecitationWordMark.UNVERIFIED
            status == RecitationWordStatus.CORRECT -> RecitationWordMark.CORRECT
            status == RecitationWordStatus.ALMOST -> RecitationWordMark.HINT
            else -> RecitationWordMark.MISTAKE
        }

    /**
     * Whether this word belongs in a mistake list or a score.
     *
     * Only a confident, scored mistake counts — which excludes both hints and unverified words.
     */
    val countsAsMistake: Boolean get() = mark == RecitationWordMark.MISTAKE

    /** Findings to show as corrections. Empty unless this word actually counts as a mistake. */
    val scorableMistakes: List<RecitationMistake>
        get() = if (countsAsMistake) mistakes else emptyList()
}
