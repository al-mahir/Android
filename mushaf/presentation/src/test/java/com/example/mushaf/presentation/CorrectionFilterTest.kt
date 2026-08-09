package com.example.mushaf.presentation

import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.recite.AyahCorrectionUi
import com.example.mushaf.presentation.recite.CorrectionFilter
import com.example.mushaf.presentation.recite.CorrectionWordUi
import com.example.mushaf.presentation.recite.MistakeFindingUi
import com.example.mushaf.presentation.recite.WordMistakeUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionFilterTest {

    private fun mistake(
        wordId: String,
        category: MistakeCategory,
    ) = WordMistakeUi(
        wordId = wordId,
        word = wordId,
        findings = listOf(
            MistakeFindingUi(
                category = category,
                labelRes = R.string.mushaf_correction_generic,
                rules = emptyList(),
                expectedLength = null,
                actualLength = null,
                expectedPhonemes = null,
                predictedPhonemes = null,
                confidencePercent = null,
                sifa = null,
            ),
        ),
    )

    private fun ayah(
        id: String,
        vararg mistakes: WordMistakeUi,
    ) = AyahCorrectionUi(
        id = id,
        sura = 1,
        aya = id.substringAfterLast(':').toInt(),
        words = listOf(CorrectionWordUi("$id:1", "w", isMistake = true)),
        mistakes = mistakes.toList(),
    )

    @Test
    fun `no corrections means no tabs at all`() {
        assertTrue(CorrectionFilter.tabsFor(emptyList()).isEmpty())
    }

    @Test
    fun `the all tab comes first and counts everything`() {
        val tabs = CorrectionFilter.tabsFor(
            listOf(
                ayah("1:3", mistake("a", MistakeCategory.TAJWID), mistake("b", MistakeCategory.TASHKIL)),
                ayah("1:7", mistake("c", MistakeCategory.TAJWID)),
            ),
        )

        assertNull("the first tab must be the unfiltered view", tabs.first().category)
        assertEquals(R.string.mushaf_correction_tab_all, tabs.first().labelRes)
        assertEquals(3, tabs.first().count)
    }

    @Test
    fun `only channels that actually occurred get a tab`() {
        
        
        val tabs = CorrectionFilter.tabsFor(
            listOf(ayah("1:3", mistake("a", MistakeCategory.TASHKIL))),
        )

        assertEquals(listOf(null, MistakeCategory.TASHKIL), tabs.map { it.category })
    }

    @Test
    fun `channel order is fixed rather than by frequency`() {
        
        val tabs = CorrectionFilter.tabsFor(
            listOf(
                ayah(
                    "1:3",
                    mistake("a", MistakeCategory.TAJWID),
                    mistake("b", MistakeCategory.TAJWID),
                    mistake("c", MistakeCategory.TAJWID),
                    mistake("d", MistakeCategory.MEMORIZATION),
                ),
            ),
        )

        assertEquals(
            listOf(null, MistakeCategory.MEMORIZATION, MistakeCategory.TAJWID),
            tabs.map { it.category },
        )
    }

    @Test
    fun `each tab counts only its own channel`() {
        val tabs = CorrectionFilter.tabsFor(
            listOf(
                ayah(
                    "1:3",
                    mistake("a", MistakeCategory.TAJWID),
                    mistake("b", MistakeCategory.TAJWID),
                    mistake("c", MistakeCategory.TASHKIL),
                ),
            ),
        )

        assertEquals(2, tabs.single { it.category == MistakeCategory.TAJWID }.count)
        assertEquals(1, tabs.single { it.category == MistakeCategory.TASHKIL }.count)
    }

    @Test
    fun `the all tab applies no filtering`() {
        val corrections = listOf(ayah("1:3", mistake("a", MistakeCategory.TAJWID)))

        assertEquals(corrections, CorrectionFilter.apply(corrections, category = null))
    }

    @Test
    fun `filtering keeps only the matching mistakes`() {
        val corrections = listOf(
            ayah(
                "1:3",
                mistake("a", MistakeCategory.TAJWID),
                mistake("b", MistakeCategory.TASHKIL),
            ),
        )

        val filtered = CorrectionFilter.apply(corrections, MistakeCategory.TAJWID)

        assertEquals(1, filtered.single().mistakes.size)
        assertEquals("a", filtered.single().mistakes.single().wordId)
    }

    @Test
    fun `an ayah with nothing in the channel disappears entirely`() {
        val corrections = listOf(
            ayah("1:3", mistake("a", MistakeCategory.TAJWID)),
            ayah("1:7", mistake("b", MistakeCategory.TASHKIL)),
        )

        val filtered = CorrectionFilter.apply(corrections, MistakeCategory.TASHKIL)

        assertEquals(listOf("1:7"), filtered.map { it.id })
    }

    @Test
    fun `the ayah text stays whole when filtered`() {
        
        
        val corrections = listOf(
            ayah(
                "1:3",
                mistake("a", MistakeCategory.TAJWID),
                mistake("b", MistakeCategory.TASHKIL),
            ),
        )

        val filtered = CorrectionFilter.apply(corrections, MistakeCategory.TAJWID)

        assertEquals(corrections.single().words, filtered.single().words)
    }
}
