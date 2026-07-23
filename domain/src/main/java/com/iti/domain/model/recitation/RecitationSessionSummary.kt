package com.iti.domain.model.recitation


enum class SessionMistakeCategory {
    MEMORIZATION,

    TASHKIL,

    TAJWID,

    OTHER,
}

data class SessionMistake(
    val sura: Int,
    val aya: Int,
    val wordIndex: Int,
    val word: String,
    val category: SessionMistakeCategory,
    val ruleName: String? = null,
    val expectedLength: Int? = null,
    val actualLength: Int? = null,
)

/** Something that recurred often enough to be worth practising. */
data class SessionPracticeFocus(
    val category: SessionMistakeCategory,
    val ruleName: String?,
    val occurrences: Int,
)


data class RecitationSessionSummary(
    val id: String,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val start: SessionPosition,
    val end: SessionPosition,
    val scoredWordCount: Int,
    val mistakes: List<SessionMistake>,
    val practiceFocus: List<SessionPracticeFocus>,
) {
    val mistakeCount: Int get() = mistakes.size


    val accuracy: Float?
        get() = scoredWordCount.takeIf { it > 0 }?.let { (it - mistakeCount).toFloat() / it }

    val mistakesByCategory: Map<SessionMistakeCategory, Int>
        get() = mistakes.groupingBy { it.category }.eachCount()

    val gradedNothing: Boolean get() = scoredWordCount == 0
}

data class SessionPosition(
    val sura: Int,
    val aya: Int,
)
