package com.example.mushaf.presentation.recite

import androidx.annotation.StringRes
import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.presentation.R






 
data class CorrectionTab(
    val category: MistakeCategory?,
    @StringRes val labelRes: Int,
    val count: Int,
)








 
object CorrectionFilter {

    




 
    fun tabsFor(corrections: List<AyahCorrectionUi>): List<CorrectionTab> {
        if (corrections.isEmpty()) return emptyList()

        val counts = corrections
            .flatMap { it.mistakes }
            .groupingBy { it.category }
            .eachCount()

        val all = CorrectionTab(
            category = null,
            labelRes = R.string.mushaf_correction_tab_all,
            count = counts.values.sum(),
        )

        
        
        val channels = ORDER.mapNotNull { category ->
            counts[category]?.let { CorrectionTab(category, category.labelRes(), it) }
        }

        return listOf(all) + channels
    }

    





 
    fun apply(
        corrections: List<AyahCorrectionUi>,
        category: MistakeCategory?,
    ): List<AyahCorrectionUi> {
        if (category == null) return corrections
        return corrections.mapNotNull { correction ->
            val matching = correction.mistakes.filter { it.category == category }
            if (matching.isEmpty()) null else correction.copy(mistakes = matching)
        }
    }

    @StringRes
    private fun MistakeCategory.labelRes(): Int = when (this) {
        MistakeCategory.MEMORIZATION -> R.string.mushaf_correction_tab_memorization
        MistakeCategory.TASHKIL -> R.string.mushaf_correction_tashkeel
        MistakeCategory.TAJWID -> R.string.mushaf_correction_tajweed
        MistakeCategory.OTHER -> R.string.mushaf_correction_generic
    }

    private val ORDER = listOf(
        MistakeCategory.MEMORIZATION,
        MistakeCategory.TASHKIL,
        MistakeCategory.TAJWID,
        MistakeCategory.OTHER,
    )
}
