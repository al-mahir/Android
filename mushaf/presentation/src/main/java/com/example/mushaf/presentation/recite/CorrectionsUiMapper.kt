package com.example.mushaf.presentation.recite

import androidx.annotation.StringRes
import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.domain.model.recite.RecitationMistake
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.SpeechErrorType
import com.example.mushaf.presentation.R

/**
 * One āyah's worth of corrections, ready to render.
 *
 * Carries string *resources* rather than resolved text, so the list can be built and tested off
 * the main thread and still read correctly in both locales (AGENTS.md → Localization).
 */
data class AyahCorrectionUi(
    val id: String,
    val sura: Int,
    val aya: Int,
    /** The whole āyah, for context — a flagged word on its own is unreadable. */
    val words: List<CorrectionWordUi>,
    /** One entry per mistaken word, so nothing the reciter got wrong is hidden behind a summary. */
    val mistakes: List<WordMistakeUi>,
) {
    /** The word to focus on the page when the card is opened. */
    val firstMistakeWordId: String get() = mistakes.first().wordId
}

/** A single mistaken word, with what was wrong with it. */
data class WordMistakeUi(
    val wordId: String,
    val word: String,
    @StringRes val labelRes: Int,
    val detail: MistakeDetailUi?,
)

data class CorrectionWordUi(
    val wordId: String,
    val text: String,
    val isMistake: Boolean,
)

/**
 * A concrete, checkable explanation — "Normal Madd: expected 2, you held 3".
 *
 * Only built when the finding actually carries a rule and both lengths. A finding without them
 * gets no detail rather than invented prose: telling a reciter something specific and wrong about
 * their tajwīd is worse than telling them nothing.
 */
data class MistakeDetailUi(
    val ruleName: String,
    val expectedLength: Int,
    val actualLength: Int,
)

object CorrectionsUiMapper {

    /**
     * Groups the session's confident mistakes by āyah, in recitation order.
     *
     * Only words that [RecitationWordFeedback.countsAsMistake] are treated as mistakes. Hints and
     * unverified words still appear as *context* inside their āyah — the reciter needs to read
     * the surrounding words to make sense of the correction — but never as an accusation.
     */
    fun toCorrections(wordFeedback: Map<String, RecitationWordFeedback>): List<AyahCorrectionUi> =
        wordFeedback.values
            .groupBy { it.position.sura to it.position.aya }
            .toSortedMap(compareBy({ it.first }, { it.second }))
            .mapNotNull { (key, words) ->
                val ordered = words.sortedBy { it.position.wordIndex }
                val mistakes = ordered.filter { it.countsAsMistake }
                if (mistakes.isEmpty()) return@mapNotNull null

                val (sura, aya) = key
                AyahCorrectionUi(
                    id = "$sura:$aya",
                    sura = sura,
                    aya = aya,
                    words = ordered.map {
                        CorrectionWordUi(
                            wordId = it.wordId,
                            text = it.uthmani,
                            isMistake = it.countsAsMistake,
                        )
                    },
                    mistakes = mistakes.map { word ->
                        WordMistakeUi(
                            wordId = word.wordId,
                            word = word.uthmani,
                            labelRes = labelFor(word),
                            detail = word.scorableMistakes.toDetail(),
                        )
                    },
                )
            }

    /**
     * What went wrong with this particular word.
     *
     * A word can carry more than one finding; the first is used, since the rows are already
     * per-word and a stack of labels on one word reads as noise rather than detail.
     */
    @StringRes
    private fun labelFor(word: RecitationWordFeedback): Int {
        val finding = word.scorableMistakes.firstOrNull()
            ?: return R.string.mushaf_correction_generic

        return when (finding.category) {
            MistakeCategory.MEMORIZATION -> when (finding.speechErrorType) {
                SpeechErrorType.INSERT -> R.string.mushaf_correction_extra_words
                SpeechErrorType.DELETE -> R.string.mushaf_correction_missing_words
                SpeechErrorType.REPLACE -> R.string.mushaf_correction_wrong_words
                SpeechErrorType.UNKNOWN -> R.string.mushaf_correction_generic
            }

            MistakeCategory.TASHKIL -> R.string.mushaf_correction_tashkeel
            MistakeCategory.TAJWID -> R.string.mushaf_correction_tajweed
            MistakeCategory.OTHER -> R.string.mushaf_correction_generic
        }
    }

    /** The first finding that can be explained precisely, or null if none can. */
    private fun List<RecitationMistake>.toDetail(): MistakeDetailUi? =
        firstNotNullOfOrNull { mistake ->
            val rule = mistake.rules.firstOrNull() ?: return@firstNotNullOfOrNull null
            val expected = mistake.expectedLength ?: return@firstNotNullOfOrNull null
            val actual = mistake.actualLength ?: return@firstNotNullOfOrNull null
            MistakeDetailUi(
                ruleName = rule.nameArabic,
                expectedLength = expected,
                actualLength = actual,
            )
        }
}
