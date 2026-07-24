package com.example.mushaf.domain.model.recite.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicTextNormalizerTest {

    @Test
    fun `hamza-on-alif variants and bare alif all fold to the same form`() {
        val expected = ArabicTextNormalizer.normalize("احمد")
        assertEquals(expected, ArabicTextNormalizer.normalize("أحمد"))
        assertEquals(expected, ArabicTextNormalizer.normalize("إحمد"))
        assertEquals(expected, ArabicTextNormalizer.normalize("آحمد"))
    }

    @Test
    fun `tashkil is stripped`() {
        assertEquals(
            ArabicTextNormalizer.normalize("الرحمن"),
            ArabicTextNormalizer.normalize("الرَّحْمَٰنِ"),
        )
    }

    @Test
    fun `ta marbuta folds to ha and alif maqsura folds to ya`() {
        assertEquals("رحمه", ArabicTextNormalizer.normalize("رحمة"))
        assertEquals("علي", ArabicTextNormalizer.normalize("علي".replace('ي', 'ى')))
    }

    @Test
    fun `tatweel is removed entirely`() {
        assertEquals(ArabicTextNormalizer.normalize("الله"), ArabicTextNormalizer.normalize("اللـه"))
    }

    @Test
    fun `whitespace runs collapse and edges trim`() {
        assertEquals("بسم الله", ArabicTextNormalizer.normalize("  بسم   الله  "))
    }

    @Test
    fun `allah ligature expands`() {
        assertEquals(ArabicTextNormalizer.normalize("الله"), ArabicTextNormalizer.normalize("ﷲ"))
    }

    @Test
    fun `already-plain quran corpus text is left effectively unchanged`() {
        // Matches the shape of the plain text bundled in quran_text.db (surah 112).
        assertEquals("قل هو الله احد", ArabicTextNormalizer.normalize("قل هو الله احد"))
    }
}
