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

        // Counted per finding, not per word: a word flagged for both tashkīl and tajwīd is one
        // entry under each channel, and the totals then match what each tab actually lists.
        val counts = corrections
            .flatMap { it.mistakes }
            .flatMap { it.findings }
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
            // Narrow to the matching findings too, so a word kept for its tajwīd error does not
            // also display its unrelated tashkīl one while the tajwīd tab is selected.
            val matching = correction.mistakes.mapNotNull { mistake ->
                val findings = mistake.findings.filter { it.category == category }
                if (findings.isEmpty()) null else mistake.copy(findings = findings)
            }
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
