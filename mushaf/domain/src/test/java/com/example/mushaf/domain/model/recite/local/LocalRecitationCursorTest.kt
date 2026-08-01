package com.example.mushaf.domain.model.recite.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalRecitationCursorTest {

    private val window = listOf(
        LocalWordEntry("1:1:1", "بسم"),
        LocalWordEntry("1:1:2", "الله"),
        LocalWordEntry("1:1:3", "الرحمن"),
        LocalWordEntry("1:1:4", "الرحيم"),
    )

    @Test
    fun `resolves a clean match to its index`() {
        assertEquals(2, LocalRecitationCursor.resolve("الرحمن", window, biasIndex = 0))
    }

    @Test
    fun `a repeated word resolves to the occurrence nearest the current position, not the first`() {
        val repeating = listOf(
            LocalWordEntry("1:1:1", "من"),
            LocalWordEntry("1:1:2", "الله"),
            LocalWordEntry("1:1:3", "من"),
            LocalWordEntry("1:1:4", "الرحمن"),
        )
        // Biased near index 3 (the reciter is believed to be near the end of the window) - the
        // second "من" at index 2 should win over the first one at index 0.
        assertEquals(2, LocalRecitationCursor.resolve("من", repeating, biasIndex = 3))
        assertEquals(0, LocalRecitationCursor.resolve("من", repeating, biasIndex = 0))
    }

    @Test
    fun `no match anywhere in the window returns null`() {
        assertNull(LocalRecitationCursor.resolve("قلم", window, biasIndex = 0))
    }
}
