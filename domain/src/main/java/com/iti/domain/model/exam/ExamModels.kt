package com.iti.domain.model.exam

sealed interface ExamScope {
    data class SingleSurah(val surahNumber: Int) : ExamScope
    data class SurahRange(val fromSurah: Int, val toSurah: Int) : ExamScope
    data class MultiSurah(val surahNumbers: List<Int>) : ExamScope
    data class SingleJuz(val juzNumber: Int) : ExamScope
    data class JuzRange(val fromJuz: Int, val toJuz: Int) : ExamScope
    data class SingleRub(val rubNumber: Int) : ExamScope
    data class AyahRange(val startSurah: Int, val startAyah: Int, val endSurah: Int, val endAyah: Int) : ExamScope
    data class CustomRange(val id: String, val name: String, val components: List<ExamScope>) : ExamScope
}

data class ExamQuestion(
    val index: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val endSurahNumber: Int = surahNumber,
    val endAyahNumber: Int = ayahNumber,
    val lineCount: Int = 0,
    val uthmaniText: String = "",
)

enum class WordStatus { CORRECT, ALMOST, ERROR, TRIMMED, PENDING }

data class WordFeedback(
    val uthmani: String,
    val status: WordStatus,
    val errorLabels: List<String> = emptyList(),
    val trimmed: Boolean = false,
    val primaryErrorType: String? = null,
    val surah: Int = 1,
    val ayah: Int = 1,
    val wordIdx: Int = 0
)

enum class QuestionStatus { PENDING, CORRECT, TAJWEED_MISTAKE, WORD_MISTAKE, SKIPPED }

enum class ExamMistakeCategory { MEMORIZATION, TASHKEEL, TAJWEED, OTHER }

data class ExamMistake(
    val surahNumber: Int,
    val ayahNumber: Int,
    val wordIndex: Int,
    val word: String,
    val category: ExamMistakeCategory,
    val expectedText: String? = null,
    val predictedText: String? = null,
    val ruleName: String? = null,
)

data class ExamQuestionResult(
    val question: ExamQuestion,
    val words: List<WordFeedback> = emptyList(),
    val mistakes: List<ExamMistake> = emptyList(),
    val durationMs: Long = 0L,
    val skipped: Boolean = false,
) {
    val mistakeCount: Int get() = mistakes.size

    // FIXED: Requires at least one correct word to be considered a successful attempt
    val isCorrect: Boolean get() = !skipped && mistakes.isEmpty() && words.any { it.status == WordStatus.CORRECT }

    val questionStatus: QuestionStatus get() = when {
        skipped -> QuestionStatus.SKIPPED
        words.none { it.status != WordStatus.PENDING } -> QuestionStatus.PENDING
        mistakes.any { it.category == ExamMistakeCategory.MEMORIZATION || it.category == ExamMistakeCategory.TASHKEEL } -> QuestionStatus.WORD_MISTAKE
        mistakes.isNotEmpty() -> QuestionStatus.TAJWEED_MISTAKE
        else -> QuestionStatus.CORRECT
    }
}

data class ExamSummary(
    val id: String,
    val scope: ExamScope,
    val startedAtMs: Long,
    val totalDurationMs: Long,
    val questionResults: List<ExamQuestionResult>,
) {
    val totalQuestions: Int get() = questionResults.size
    val correctCount: Int get() = questionResults.count { it.isCorrect }
    val mistakeCount: Int get() = questionResults.sumOf { it.mistakeCount }
    val skippedCount: Int get() = questionResults.count { it.skipped }
    val allMistakes: List<ExamMistake> get() = questionResults.flatMap { it.mistakes }
    val accuracy: Float get() = if (totalQuestions == 0) 0f else correctCount.toFloat() / totalQuestions
    val mistakesByCategory: Map<ExamMistakeCategory, Int> get() = allMistakes.groupingBy { it.category }.eachCount()
}

data class RecentExamScope(
    val id: String,
    val scope: ExamScope,
    val displayLabel: String,
    val timestampMs: Long,
)