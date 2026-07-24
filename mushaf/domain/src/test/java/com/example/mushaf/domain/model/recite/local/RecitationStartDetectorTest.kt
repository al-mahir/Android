package com.example.mushaf.domain.model.recite.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecitationStartDetectorTest {

    private val fatiha = listOf(
        LocalWordEntry("1:1:1", "بسم"),
        LocalWordEntry("1:1:2", "الله"),
        LocalWordEntry("1:1:3", "الرحمن"),
        LocalWordEntry("1:1:4", "الرحيم"),
        LocalWordEntry("1:2:1", "الحمد"),
        LocalWordEntry("1:2:2", "لله"),
        LocalWordEntry("1:2:3", "رب"),
        LocalWordEntry("1:2:4", "العالمين"),
    )

    @Test
    fun `a single word is not enough to commit`() {
        val detector = RecitationStartDetector(fatiha)
        assertNull(detector.offer("الحمد"))
    }

    @Test
    fun `two consecutive matching words lock onto the start of that run, not the second word`() {
        val detector = RecitationStartDetector(fatiha)
        assertNull(detector.offer("الحمد"))
        val result = detector.offer("لله")
        assertEquals("1:2:1", result?.wordId)
    }

    @Test
    fun `starting from the very first word of the page still locks correctly`() {
        val detector = RecitationStartDetector(fatiha)
        detector.offer("بسم")
        val result = detector.offer("الله")
        assertEquals("1:1:1", result?.wordId)
    }

    @Test
    fun `an ambiguous first word is resolved once the second word disambiguates`() {
        // "الله" alone matches both 1:1:2 and 1:2:2 ("لله" normalizes toward "الله"-ish but
        // let's use a genuinely repeated word instead - reuse "الرحمن" style setup with an
        // explicit duplicate for clarity.
        val withDuplicate = fatiha + LocalWordEntry("1:3:1", "الحمد")
        val detector = RecitationStartDetector(withDuplicate)
        // "الحمد" matches both index 4 (1:2:1) and index 8 (1:3:1).
        assertNull(detector.offer("الحمد"))
        // Only the chain starting at 1:2:1 continues with "لله" next.
        val result = detector.offer("لله")
        assertEquals("1:2:1", result?.wordId)
    }

    @Test
    fun `pure noise that never matches anything gives up after the word cap and falls back to page start`() {
        val detector = RecitationStartDetector(fatiha, maxWordsConsidered = 3)
        assertNull(detector.offer("قطة"))
        assertNull(detector.offer("سيارة"))
        val result = detector.offer("شجرة")
        assertEquals("1:1:1", result?.wordId)
    }

    @Test
    fun `once decided, further offers are no-ops`() {
        val detector = RecitationStartDetector(fatiha)
        detector.offer("الحمد")
        val locked = detector.offer("لله")
        assertEquals("1:2:1", locked?.wordId)
        assertNull(detector.offer("رب"))
    }

    @Test
    fun `empty window never crashes and never locks`() {
        val detector = RecitationStartDetector(emptyList())
        assertNull(detector.offer("بسم"))
    }
}
