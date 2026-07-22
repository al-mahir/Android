package com.example.mushaf.domain

import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.RecitationWordMark
import com.example.mushaf.domain.model.recite.RecitationWordStatus
import com.example.mushaf.domain.model.recite.mergedWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecitationCursorTest {

    @Test
    fun `word ids round-trip through the 0-based to 1-based conversion`() {
        val cursor = RecitationCursor(sura = 2, aya = 255, wordIndex = 4)

        assertEquals("2:255:5", cursor.wordId)
        assertEquals(cursor, RecitationCursor.fromWordId(cursor.wordId))
    }

    @Test
    fun `the first word of an ayah is index zero and key one`() {
        assertEquals(RecitationCursor(1, 1, 0), RecitationCursor.fromWordId("1:1:1"))
    }

    @Test
    fun `a malformed key yields no cursor rather than a guess`() {
        
        
        assertNull(RecitationCursor.fromWordId("p1:l2:p3"))
        assertNull(RecitationCursor.fromWordId("1:1"))
        assertNull(RecitationCursor.fromWordId(""))
        assertNull(RecitationCursor.fromWordId("1:1:0"))
        assertNull(RecitationCursor.fromWordId("0:1:1"))
    }
}






 
class RecitationLedgerTest {

    private fun word(
        wordIndex: Int,
        status: RecitationWordStatus = RecitationWordStatus.CORRECT,
        trimmed: Boolean = false,
    ) = RecitationWordFeedback(
        position = RecitationCursor(1, 1, wordIndex),
        uthmani = "w$wordIndex",
        status = status,
        mistakes = emptyList(),
        isTrimmed = trimmed,
    )

    private fun ledgerOf(vararg words: RecitationWordFeedback) = words.associateBy { it.wordId }

    @Test
    fun `new words are added`() {
        val merged = ledgerOf(word(0)).mergedWith(ledgerOf(word(1)))

        assertEquals(setOf("1:1:1", "1:1:2"), merged.keys)
    }

    @Test
    fun `a rescored word is updated`() {
        val existing = ledgerOf(word(0, RecitationWordStatus.ALMOST))

        val merged = existing.mergedWith(ledgerOf(word(0, RecitationWordStatus.ERROR)))

        assertEquals(RecitationWordMark.MISTAKE, merged.getValue("1:1:1").mark)
    }

    @Test
    fun `a trimmed report never overwrites a scored verdict`() {
        val existing = ledgerOf(word(0, RecitationWordStatus.ERROR, trimmed = false))

        val merged = existing.mergedWith(ledgerOf(word(0, RecitationWordStatus.CORRECT, trimmed = true)))

        assertFalse("an unscored report erased a real verdict", merged.getValue("1:1:1").isTrimmed)
        assertEquals(RecitationWordMark.MISTAKE, merged.getValue("1:1:1").mark)
    }

    @Test
    fun `a scored report does replace an earlier trimmed one`() {
        
        
        val existing = ledgerOf(word(0, RecitationWordStatus.CORRECT, trimmed = true))

        val merged = existing.mergedWith(ledgerOf(word(0, RecitationWordStatus.ERROR, trimmed = false)))

        assertEquals(RecitationWordMark.MISTAKE, merged.getValue("1:1:1").mark)
    }

    @Test
    fun `a trimmed report still fills a gap`() {
        val merged = emptyMap<String, RecitationWordFeedback>()
            .mergedWith(ledgerOf(word(0, trimmed = true)))

        assertTrue(merged.containsKey("1:1:1"))
        assertEquals(RecitationWordMark.UNVERIFIED, merged.getValue("1:1:1").mark)
    }

    @Test
    fun `an empty chunk leaves the ledger untouched`() {
        val existing = ledgerOf(word(0))

        assertEquals(existing, existing.mergedWith(emptyMap()))
    }
}
