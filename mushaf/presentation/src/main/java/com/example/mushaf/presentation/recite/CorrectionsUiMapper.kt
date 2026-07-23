package com.example.mushaf.presentation.recite

import androidx.annotation.StringRes
import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.domain.model.recite.RecitationMistake
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.SpeechErrorType
import com.example.mushaf.presentation.R






 
data class AyahCorrectionUi(
    val id: String,
    val sura: Int,
    val aya: Int,
     
    val words: List<CorrectionWordUi>,
     
    val mistakes: List<WordMistakeUi>,
) {
     
    val firstMistakeWordId: String get() = mistakes.first().wordId
}

 
data class WordMistakeUi(
    val wordId: String,
    val word: String,
    val category: MistakeCategory,
    @StringRes val labelRes: Int,
    val detail: MistakeDetailUi?,
)

data class CorrectionWordUi(
    val wordId: String,
    val text: String,
    val isMistake: Boolean,
)







 
data class MistakeDetailUi(
    val ruleName: String,
    val expectedLength: Int,
    val actualLength: Int,
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
                    mistakes = mistakes.map { word ->
                        WordMistakeUi(
                            wordId = word.wordId,
                            word = word.uthmani,
                            category = word.scorableMistakes.firstOrNull()?.category
                                ?: MistakeCategory.OTHER,
                            labelRes = labelFor(word),
                            detail = word.scorableMistakes.toDetail(),
                        )
                    },
                )
            }

    




 
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
