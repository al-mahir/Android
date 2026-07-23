package com.example.mushaf.domain.model.recite

import com.iti.domain.model.recitation.RecitationSessionSummary
import com.iti.domain.model.recitation.SessionMistake
import com.iti.domain.model.recitation.SessionMistakeCategory
import com.iti.domain.model.recitation.SessionPosition
import com.iti.domain.model.recitation.SessionPracticeFocus


object RecitationSessionRecorder {

    fun record(
        id: String,
        startedAtEpochMs: Long,
        durationMs: Long,
        wordFeedback: Map<String, RecitationWordFeedback>,
        fallbackPosition: RecitationCursor?,
    ): RecitationSessionSummary {
        val ordered = wordFeedback.values.sortedWith(
            compareBy({ it.position.sura }, { it.position.aya }, { it.position.wordIndex }),
        )
        val first = ordered.firstOrNull()?.position ?: fallbackPosition
        val last = ordered.lastOrNull()?.position ?: fallbackPosition

        return RecitationSessionSummary(
            id = id,
            startedAtEpochMs = startedAtEpochMs,
            durationMs = durationMs,
            start = first.toSessionPosition(),
            end = last.toSessionPosition(),
            
            
            scoredWordCount = ordered.count { it.mark != RecitationWordMark.UNVERIFIED },
            mistakes = ordered.filter { it.countsAsMistake }.map { it.toSessionMistake() },
            practiceFocus = ordered.practiceFocus().map { it.toSessionPracticeFocus() },
        )
    }

    private fun RecitationCursor?.toSessionPosition() =
        SessionPosition(sura = this?.sura ?: 0, aya = this?.aya ?: 0)

    private fun RecitationWordFeedback.toSessionMistake(): SessionMistake {
        val finding = scorableMistakes.firstOrNull()
        return SessionMistake(
            sura = position.sura,
            aya = position.aya,
            wordIndex = position.wordIndex,
            word = uthmani,
            category = finding?.category.toSessionCategory(),
            ruleName = finding?.rules?.firstOrNull()?.nameArabic,
            expectedLength = finding?.expectedLength,
            actualLength = finding?.actualLength,
        )
    }

    private fun PracticeFocus.toSessionPracticeFocus() = SessionPracticeFocus(
        category = category.toSessionCategory(),
        ruleName = ruleName,
        occurrences = occurrences,
    )

    




 
    private fun MistakeCategory?.toSessionCategory(): SessionMistakeCategory = when (this) {
        MistakeCategory.MEMORIZATION -> SessionMistakeCategory.MEMORIZATION
        MistakeCategory.TASHKIL -> SessionMistakeCategory.TASHKIL
        MistakeCategory.TAJWID -> SessionMistakeCategory.TAJWID
        MistakeCategory.OTHER, null -> SessionMistakeCategory.OTHER
    }
}
