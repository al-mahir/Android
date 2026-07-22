package com.example.mushaf.presentation.recite

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.mushaf.CorrectionCardUi
import com.example.designsystem.components.mushaf.CorrectionMistakeUi
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.SurahNameResolver
import com.example.mushaf.presentation.state.LiveCorrectionUiState
import com.example.designsystem.components.mushaf.CorrectionWordUi as DesignCorrectionWordUi







 
@Composable
fun AyahCorrectionUi.toCard(): CorrectionCardUi = CorrectionCardUi(
    id = id,
    ayahLabel = stringResource(R.string.mushaf_ayah_label, sura, aya),
    words = words.map { DesignCorrectionWordUi(text = it.text, isMistake = it.isMistake) },
    mistakes = mistakes.map { mistake ->
        CorrectionMistakeUi(
            wordId = mistake.wordId,
            word = mistake.word,
            label = stringResource(mistake.labelRes),
            detail = mistake.detail?.let {
                stringResource(
                    R.string.mushaf_correction_detail,
                    it.ruleName,
                    it.expectedLength,
                    it.actualLength,
                )
            },
        )
    },
)







 
@Composable
fun LiveCorrectionUiState.correctionsSubtitle(): String {
    val count = pluralStringResource(R.plurals.mushaf_corrections_count, mistakeCount, mistakeCount)
    val first = wordFeedback.values.minByOrNull { it.position.sura * 1000 + it.position.aya }
    val last = wordFeedback.values.maxByOrNull { it.position.sura * 1000 + it.position.aya }
    if (first == null || last == null) return count

    fun label(sura: Int, aya: Int) = "${SurahNameResolver.nameFor(sura)} $aya"
    val range = stringResource(
        R.string.mushaf_corrections_range,
        label(first.position.sura, first.position.aya),
        label(last.position.sura, last.position.aya),
    )
    return "$count · $range"
}
