package com.example.mushaf.presentation.recite

import androidx.annotation.StringRes
import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.domain.model.recite.RecitationMistake
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.SifaComparison
import com.example.mushaf.domain.model.recite.SpeechErrorType
import com.example.mushaf.domain.model.recite.TajweedRuleReference
import com.example.mushaf.presentation.R
import kotlin.math.roundToInt






data class AyahCorrectionUi(
    val id: String,
    val sura: Int,
    val aya: Int,

    val words: List<CorrectionWordUi>,

    val mistakes: List<WordMistakeUi>,
) {

    val firstMistakeWordId: String get() = mistakes.first().wordId
}

/**
 * One mistaken word and *every* finding the engine reported against it.
 *
 * A word can fail on several channels at once — a tashkīl slip and a madd length in the same word
 * are two separate findings — so this holds a list. Collapsing to the first finding, as this used
 * to, silently hid the rest.
 */
data class WordMistakeUi(
    val wordId: String,
    val word: String,
    val findings: List<MistakeFindingUi>,
) {
    /** Every channel this word was flagged on — one word can belong to more than one filter tab. */
    val categories: Set<MistakeCategory> get() = findings.mapTo(LinkedHashSet()) { it.category }
}

data class CorrectionWordUi(
    val wordId: String,
    val text: String,
    val isMistake: Boolean,
)

/**
 * A single finding, carrying everything the backend reported about it rather than only its type.
 *
 * Fields are null/empty exactly when the engine omitted them — a memorization slip carries no
 * tajwīd rule or length, a length error carries no phonemes — so the UI renders what is present
 * and stays silent about the rest.
 */
data class MistakeFindingUi(
    val category: MistakeCategory,
    @StringRes val labelRes: Int,
    val rules: List<TajweedRuleReference>,
    val expectedLength: Int?,
    val actualLength: Int?,
    val expectedPhonemes: String?,
    val predictedPhonemes: String?,
    val confidencePercent: Int?,
    /**
     * Present on ṣifāt findings. When set it replaces the phoneme line, because on that channel
     * the phoneme fields hold `attribute=value` tokens rather than anything readable.
     */
    val sifa: SifaComparison?,
)

object CorrectionsUiMapper {

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
                    mistakes = mistakes.map { it.toMistakeUi() },
                )
            }

    private fun RecitationWordFeedback.toMistakeUi() = WordMistakeUi(
        wordId = wordId,
        word = uthmani,
        // A flagged word with no itemised findings still deserves a row, so fall back to a
        // single generic finding rather than rendering an empty one.
        findings = scorableMistakes.map { it.toFinding() }.ifEmpty { listOf(GENERIC_FINDING) },
    )

    private fun RecitationMistake.toFinding() = MistakeFindingUi(
        category = category,
        labelRes = labelFor(category, speechErrorType),
        rules = rules,
        expectedLength = expectedLength,
        actualLength = actualLength,
        // A parsed ṣifā owns these fields; showing the raw `attribute=value` tokens as well
        // would just repeat the same fact in its unreadable form.
        expectedPhonemes = expectedPhonemes?.takeIf { sifa == null && it.isNotBlank() },
        predictedPhonemes = predictedPhonemes?.takeIf { sifa == null && it.isNotBlank() },
        confidencePercent = confidence?.let { (it * 100).roundToInt() },
        sifa = sifa,
    )

    @StringRes
    private fun labelFor(category: MistakeCategory, speechErrorType: SpeechErrorType): Int =
        when (category) {
            MistakeCategory.MEMORIZATION -> when (speechErrorType) {
                SpeechErrorType.INSERT -> R.string.mushaf_correction_extra_words
                SpeechErrorType.DELETE -> R.string.mushaf_correction_missing_words
                SpeechErrorType.REPLACE -> R.string.mushaf_correction_wrong_words
                SpeechErrorType.UNKNOWN -> R.string.mushaf_correction_generic
            }

            MistakeCategory.TASHKIL -> R.string.mushaf_correction_tashkeel
            MistakeCategory.TAJWID -> R.string.mushaf_correction_tajweed
            MistakeCategory.OTHER -> R.string.mushaf_correction_generic
        }

    private val GENERIC_FINDING = MistakeFindingUi(
        category = MistakeCategory.OTHER,
        labelRes = R.string.mushaf_correction_generic,
        rules = emptyList(),
        expectedLength = null,
        actualLength = null,
        expectedPhonemes = null,
        predictedPhonemes = null,
        confidencePercent = null,
        sifa = null,
    )
}
