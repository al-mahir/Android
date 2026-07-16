package com.example.mushaf.data

import com.example.mushaf.data.db.MushafLineEntity
import com.example.mushaf.data.mapper.MushafMapper
import com.example.mushaf.domain.model.LineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafMapperTest {

    // Helper building an entity for page 1.
    private fun row(line: Int, type: String, centered: Int, first: Int?, last: Int?, surah: Int?) =
        MushafLineEntity(
            pageNumber = 1,
            lineNumber = line,
            lineType = type,
            isCentered = centered,
            firstWordId = first,
            lastWordId = last,
            surahNumber = surah,
        )

    @Test
    fun `ayah line expands word id range into ordered words`() {
        val rows = listOf(row(2, "ayah", 1, 1, 5, null))

        val page = MushafMapper.toDomain(1, rows)
        val words = page.lines.single().words

        assertEquals(5, words.size)
        assertEquals(listOf(1, 2, 3, 4, 5), words.map { it.wordId })
        assertEquals(listOf(0, 1, 2, 3, 4), words.map { it.positionInLine })
        assertEquals("p1:l2:w1", words.first().id)
    }

    @Test
    fun `glyph code is page-relative from the page's first word`() {
        val rows = listOf(row(2, "ayah", 1, 1, 3, null))

        val words = MushafMapper.toDomain(1, rows).lines.single().words

        // pageFirstWordId = 1; word-glyph block begins at U+FC41 (verified against font cmap)
        assertEquals(0xFC41, words.first().glyphCode)
        assertEquals(0xFC43, words.last().glyphCode)
    }

    @Test
    fun `surah name and basmallah lines have no words`() {
        val rows = listOf(
            row(1, "surah_name", 1, null, null, 1),
            row(2, "basmallah", 1, null, null, null),
            row(3, "ayah", 1, 1, 4, null),
        )

        val page = MushafMapper.toDomain(1, rows)

        val surah = page.lines.first { it.type == LineType.SURAH_NAME }
        val basmallah = page.lines.first { it.type == LineType.BASMALLAH }
        assertTrue(surah.words.isEmpty())
        assertEquals(1, surah.surahNumber)
        assertTrue(basmallah.words.isEmpty())
        assertEquals(4, page.lines.first { it.type == LineType.AYAH }.words.size)
    }

    @Test
    fun `lines are ordered by line number`() {
        val rows = listOf(
            row(3, "ayah", 1, 6, 10, null),
            row(1, "surah_name", 1, null, null, 1),
            row(2, "ayah", 1, 1, 5, null),
        )

        val page = MushafMapper.toDomain(1, rows)

        assertEquals(listOf(1, 2, 3), page.lines.map { it.lineNumber })
    }
}
