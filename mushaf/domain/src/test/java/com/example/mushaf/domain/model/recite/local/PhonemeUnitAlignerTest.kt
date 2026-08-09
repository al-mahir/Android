package com.example.mushaf.domain.model.recite.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixtures are real rows: the phoneme units come from `ordered_quran_phonemes.json` and the words
 * from the app's own `quran_text.db`, so these exercise the actual pair of spellings the aligner
 * has to reconcile rather than a tidied-up version of them.
 */
class PhonemeUnitAlignerTest {

    private fun wordsOf(sura: Int, aya: Int, text: String): List<LocalWordEntry> =
        text.split(" ").mapIndexed { index, word ->
            LocalWordEntry(wordId = "$sura:$aya:${index + 1}", plainText = word)
        }

    @Test
    fun `one unit per word when nothing merges`() {
        val units = listOf("بِسمِ", "للَااهِ", "ررَحمَاانِ", "ررَحِۦۦۦۦم")
        val words = wordsOf(1, 1, "بسم الله الرحمن الرحيم")

        val aligned = PhonemeUnitAligner.align(units, words)

        assertEquals(4, aligned.size)
        assertEquals(listOf("1:1:1", "1:1:2", "1:1:3", "1:1:4"), aligned.map { it.lastWordId })
        assertTrue("no unit should have claimed two words", aligned.all { it.wordIds.size == 1 })
    }

    @Test
    fun `a tajweed merge claims both of its words and lands the cursor on the second`() {
        // 2:2 ends "هُدًۭى لِّلْمُتَّقِينَ" — two written words, one uninterrupted sound.
        val units = listOf("ذَاالِكَ", "لكِتَاابُ", "لَاا", "رَيبَ", "فِۦۦهِ", "هُدَللِلمُتتَقِۦۦۦۦن")
        val words = wordsOf(2, 2, "ذلك الكتاب لا ريب فيه هدى للمتقين")

        val aligned = PhonemeUnitAligner.align(units, words)

        assertEquals(6, aligned.size)
        assertEquals(listOf("2:2:6", "2:2:7"), aligned.last().wordIds)
        assertEquals(
            "the highlight must land on the last word of a merged span, not the first",
            "2:2:7",
            aligned.last().lastWordId,
        )
        assertEquals(listOf("2:2:1", "2:2:2", "2:2:3", "2:2:4", "2:2:5"), aligned.dropLast(1).map { it.lastWordId })
    }

    @Test
    fun `the alignment always covers every word exactly once`() {
        val units = listOf("ءَلحَمدُ", "لِللَااهِ", "رَببِ", "لعَاالَمِۦۦۦۦن")
        val words = wordsOf(1, 2, "الحمد لله رب العالمين")

        val aligned = PhonemeUnitAligner.align(units, words)

        assertEquals(words.map { it.wordId }, aligned.flatMap { it.wordIds })
    }

    @Test
    fun `more units than words cannot describe the same ayah and is refused`() {
        val aligned = PhonemeUnitAligner.align(
            units = listOf("بِسمِ", "للَااهِ", "ررَحمَاانِ"),
            words = wordsOf(1, 1, "بسم الله"),
        )

        assertTrue("a mismatched row must yield no tracking, never a guessed mapping", aligned.isEmpty())
    }

    @Test
    fun `empty input is refused rather than crashing`() {
        assertTrue(PhonemeUnitAligner.align(emptyList(), wordsOf(1, 1, "بسم")).isEmpty())
        assertTrue(PhonemeUnitAligner.align(listOf("بِسمِ"), emptyList()).isEmpty())
    }
}
