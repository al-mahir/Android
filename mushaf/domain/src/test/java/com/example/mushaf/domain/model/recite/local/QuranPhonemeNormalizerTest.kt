package com.example.mushaf.domain.model.recite.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class QuranPhonemeNormalizerTest {

    @Test
    fun `elongation and gemination collapse away`() {
        // Real reference phonemes for 1:1 word 4 — the madd is written as four small yā's.
        assertEquals("رَحِيم", QuranPhonemeNormalizer.normalize("ررَحِۦۦۦۦم"))
    }

    @Test
    fun `the small waw and small ya are letters, not marks`() {
        assertEquals("مُو", QuranPhonemeNormalizer.normalize("مُۥ"))
        assertEquals("مِي", QuranPhonemeNormalizer.normalize("مِۦ"))
    }

    @Test
    fun `noon ghunna reads as a noon`() {
        assertEquals("مِن", QuranPhonemeNormalizer.normalize("مِں"))
    }

    @Test
    fun `harakat survive normalize and are dropped by skeleton`() {
        assertEquals("بِسمِ", QuranPhonemeNormalizer.normalize("بِسمِ"))
        assertEquals("بسم", QuranPhonemeNormalizer.skeleton("بِسمِ"))
    }

    @Test
    fun `the two scripts meet in skeleton form`() {
        // Left: reference phonemes for 2:2's fifth unit. Right: the corpus's plain word.
        assertEquals(
            QuranPhonemeNormalizer.skeleton("فيه"),
            QuranPhonemeNormalizer.skeleton("فِۦۦهِ"),
        )
    }

    @Test
    fun `unrelated words do not collapse onto each other`() {
        assertNotEquals(
            QuranPhonemeNormalizer.skeleton("الرحمن"),
            QuranPhonemeNormalizer.skeleton("الرحيم"),
        )
    }

    @Test
    fun `whitespace is not a boundary the model can produce, so it is simply dropped`() {
        assertEquals("هدىللمتقين".let(QuranPhonemeNormalizer::skeleton), QuranPhonemeNormalizer.skeleton("هدى للمتقين"))
    }
}
