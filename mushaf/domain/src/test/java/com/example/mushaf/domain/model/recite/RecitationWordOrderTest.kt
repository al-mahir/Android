package com.example.mushaf.domain.model.recite

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecitationWordOrderTest {

    @Test
    fun `later words, ayahs and surahs all count as further along`() {
        assertTrue(RecitationWordOrder.isAfter("1:1:2", "1:1:1"))
        assertTrue(RecitationWordOrder.isAfter("1:2:1", "1:1:7"))
        assertTrue(RecitationWordOrder.isAfter("2:1:1", "1:7:9"))
    }

    @Test
    fun `the same word is not after itself`() {
        assertFalse(RecitationWordOrder.isAfter("1:1:1", "1:1:1"))
    }

    @Test
    fun `earlier positions are not after later ones`() {
        assertFalse(RecitationWordOrder.isAfter("1:1:1", "1:1:2"))
        assertFalse(RecitationWordOrder.isAfter("1:7:9", "2:1:1"))
    }

    @Test
    fun `an absent reference means anywhere counts as progress`() {
        assertTrue(RecitationWordOrder.isAfter("1:1:1", null))
    }

    @Test
    fun `an absent or malformed candidate is never after anything`() {
        assertFalse(RecitationWordOrder.isAfter(null, "1:1:1"))
        assertFalse(RecitationWordOrder.isAfter("not-a-word-id", "1:1:1"))
        assertFalse(RecitationWordOrder.isAfter(null, null))
    }

    @Test
    fun `a malformed reference is treated as the beginning, not as infinity`() {
        assertTrue(RecitationWordOrder.isAfter("1:1:1", "nonsense"))
    }

    @Test
    fun `word index is ordered numerically, not as text`() {
        // The trap a page-local string comparison falls into: "10" sorts before "9".
        assertTrue(RecitationWordOrder.isAfter("2:255:10", "2:255:9"))
    }

    @Test
    fun `keyOf rejects ids it cannot parse`() {
        assertNull(RecitationWordOrder.keyOf("1:1"))
        assertNull(RecitationWordOrder.keyOf(""))
        assertNull(RecitationWordOrder.keyOf(null))
    }
}
