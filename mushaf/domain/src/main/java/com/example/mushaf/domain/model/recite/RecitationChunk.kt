package com.example.mushaf.domain.model.recite

/** Recognised non-Qur'anic speech, reported so it can be acknowledged but never scored. */
enum class NonVerseSegment {
    ISTIAATHA,
    BASMALAH,
    SADAKA,
    OTHER,
}

/**
 * Whether the service could place what it heard.
 *
 * Deliberately a sealed hierarchy rather than a status enum beside a `words` list: on
 * [Ambiguous] and [NoMatch] the service is **declining to guess**, and scoring someone against
 * a verse they were not reciting is the worst failure this system has available. Modelling it
 * this way makes reading words off an unmatched chunk a compile error instead of a code review.
 */
sealed interface RecitationMatch {

    /** The passage was identified. Only here do per-word verdicts exist. */
    data class Matched(
        val words: List<RecitationWordFeedback>,
        val text: String?,
        val start: RecitationCursor?,
        val end: RecitationCursor?,
    ) : RecitationMatch

    /**
     * The passage occurs in more than one place and there was no cursor to disambiguate it.
     *
     * Show the candidates and let the reciter pick, or let the next chunk resolve it. Mark no
     * word. The basmalah is the everyday case — it matches both 1:1 and 27:30.
     */
    data class Ambiguous(val candidates: List<RecitationCandidate>) : RecitationMatch

    /**
     * Nothing matched: background noise, a non-Qur'anic utterance, or a passage too short to
     * place. Show a neutral "didn't catch that". Mark nothing wrong.
     */
    data object NoMatch : RecitationMatch
}

/**
 * One finalized waqf chunk — the unit the service grades and pushes, roughly one per pause.
 *
 * @param forcedCut the 19 s cap ended this chunk rather than a pause, so words near the boundary
 *   are less reliable and are usually trimmed. Not an error, but it explains poor-looking results.
 * @param cursor where the reciter now is. Keep the latest: it is the resume point after a drop.
 */
data class RecitationChunk(
    val sequence: Int,
    val match: RecitationMatch,
    val cursor: RecitationCursor?,
    val forcedCut: Boolean,
    val nonVerse: List<NonVerseSegment>,
) {
    /** Per-word verdicts, or empty when the service declined to place the passage. */
    val words: List<RecitationWordFeedback>
        get() = (match as? RecitationMatch.Matched)?.words.orEmpty()

    /**
     * Words that belong in a mistake list. Hints and unverified words are excluded by
     * construction, so a caller cannot accidentally count a softened finding as an accusation.
     */
    val mistakeWords: List<RecitationWordFeedback>
        get() = words.filter { it.countsAsMistake }

    /** Feedback keyed by `MushafWord.id`, ready to merge into a page's rendering state. */
    fun wordFeedbackById(): Map<String, RecitationWordFeedback> =
        words.associateBy { it.wordId }
}
