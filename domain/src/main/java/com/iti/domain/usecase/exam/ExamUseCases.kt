package com.iti.domain.usecase.exam

import com.iti.domain.model.exam.ExamMistakeCategory
import com.iti.domain.model.exam.ExamQuestion
import com.iti.domain.model.exam.ExamScope
import com.iti.domain.model.exam.ExamSummary
import com.iti.domain.model.exam.RecentExamScope
import com.iti.domain.model.quran.QuranIndex
import com.iti.domain.repository.ExamRepository
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random


class GenerateExamQuestionsUseCase {

    operator fun invoke(
        scope: ExamScope,
        count: Int,
        linesPerQuestion: Int = 3,
        seed: Long = System.currentTimeMillis(),
    ): List<ExamQuestion> {
        val pool = expandScope(scope)
        require(pool.size >= MIN_QUESTIONS) {
            "Scope must contain at least $MIN_QUESTIONS ayahs, found ${pool.size}."
        }
        require(count in MIN_QUESTIONS..MAX_QUESTIONS) {
            "count must be in [$MIN_QUESTIONS, $MAX_QUESTIONS], got $count."
        }
        val actualCount = count.coerceAtMost(pool.size)
        val rng = Random(seed)
        return pool.shuffled(rng)
            .take(actualCount)
            .sortedWith(compareBy({ it.first }, { it.second }))
            .mapIndexed { index, (surah, ayah) ->
                ExamQuestion(
                    index = index,
                    surahNumber = surah,
                    ayahNumber = ayah,
                    lineCount = linesPerQuestion
                )
            }
    }

    companion object {
        const val MIN_QUESTIONS = 3
        const val MAX_QUESTIONS = 20
    }
}

class ComputeQuestionCountRangeUseCase {

    operator fun invoke(scope: ExamScope): IntRange {
        val total = expandScope(scope).size
        return if (total < GenerateExamQuestionsUseCase.MIN_QUESTIONS) IntRange.EMPTY
        else GenerateExamQuestionsUseCase.MIN_QUESTIONS..
                total.coerceAtMost(GenerateExamQuestionsUseCase.MAX_QUESTIONS)
    }
}
class SaveExamSummaryUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(summary: ExamSummary) = repository.save(summary)
}
class GetRecentExamScopesUseCase(private val repository: ExamRepository) {
    operator fun invoke(): Flow<List<RecentExamScope>> = repository.observeRecentScopes()
}


internal fun expandScope(scope: ExamScope): List<Pair<Int, Int>> = when (scope) {
    is ExamScope.CustomRange -> scope.components.flatMap { expandScope(it) }.distinct()

    is ExamScope.SingleSurah -> expandSurah(scope.surahNumber)

    is ExamScope.SurahRange ->
        (scope.fromSurah..scope.toSurah).flatMap { expandSurah(it) }

    is ExamScope.MultiSurah ->
        scope.surahNumbers.sorted().flatMap { expandSurah(it) }

    is ExamScope.SingleJuz -> QuranIndex.ayahsInJuz(scope.juzNumber)

    is ExamScope.JuzRange ->
        (scope.fromJuz..scope.toJuz).flatMap { QuranIndex.ayahsInJuz(it) }

    is ExamScope.SingleRub -> QuranIndex.ayahsInRub(scope.rubNumber)

    is ExamScope.AyahRange -> {
        val result = mutableListOf<Pair<Int, Int>>()
        var surah = scope.startSurah
        var ayah = scope.startAyah
        while (surah < scope.endSurah || (surah == scope.endSurah && ayah <= scope.endAyah)) {
            result.add(surah to ayah)
            val maxAyah = QuranIndex.ayahCountOf(surah)
            if (ayah < maxAyah) {
                ayah++
            } else {
                surah++
                ayah = 1
            }
        }
        result
    }
}

private fun expandSurah(surahNumber: Int): List<Pair<Int, Int>> {
    val count = QuranIndex.ayahCountOf(surahNumber)
    return (1..count).map { surahNumber to it }
}


fun classifyMistake(apiErrorType: String?): ExamMistakeCategory = when (apiErrorType) {
    "normal" -> ExamMistakeCategory.MEMORIZATION
    "tashkeel" -> ExamMistakeCategory.TASHKEEL
    "tajweed", "sifa" -> ExamMistakeCategory.TAJWEED
    else -> ExamMistakeCategory.OTHER
}
