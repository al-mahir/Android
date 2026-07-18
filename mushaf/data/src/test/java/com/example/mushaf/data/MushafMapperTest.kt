package com.example.mushaf.data

import com.example.mushaf.data.db.MushafLineEntity
import com.example.mushaf.data.db.MushafWordEntity
import com.example.mushaf.data.mapper.MushafMapper
import com.example.mushaf.domain.model.LineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafMapperTest {

    private fun line(number: Int, type: String, centered: Int = 0, surah: Int? = null) =
        MushafLineEntity(
            pageNumber = 1,
            lineNumber = number,
            lineType = type,
            isCentered = centered,
            surahNumber = surah,
        )

    private fun word(line: Int, position: Int, key: String, glyphs: String, charType: String = "word") =
        MushafWordEntity(
            pageNumber = 1,
            lineNumber = line,
            position = position,
            wordKey = key,
            charType = charType,
            glyphText = glyphs,
        )

    @Test
    fun `ayah line carries its words in position order with their glyph strings`() {
        val lines = listOf(line(2, "ayah"))
        val words = listOf(
            word(2, 1, "1:1:1", "ﱁ"),
            word(2, 0, "1:1:2", "ﱂﱃ"), // out of order on purpose; carries a trailing mark glyph
        )

        val page = MushafMapper.toDomain(1, lines, words)
        val ayah = page.lines.single().words

        assertEquals(listOf(0, 1), ayah.map { it.positionInLine })
        assertEquals(listOf("ﱂﱃ", "ﱁ"), ayah.map { it.glyphs })
        assertEquals("1:1:2", ayah.first().id)
    }

    @Test
    fun `end marker token is flagged as end of ayah`() {
        val lines = listOf(line(2, "ayah"))
        val words = listOf(
            word(2, 0, "1:1:1", "ﱁ"),
            word(2, 1, "1:1:2", "ﱂ", charType = "end"),
        )

        val words2 = MushafMapper.toDomain(1, lines, words).lines.single().words
        assertFalse(words2[0].isEndOfAyah)
        assertTrue(words2[1].isEndOfAyah)
    }

    @Test
    fun `surah name and basmallah lines have no words`() {
        val lines = listOf(
            line(1, "surah_name", centered = 1, surah = 1),
            line(2, "basmallah", centered = 1),
            line(3, "ayah"),
        )
        val words = listOf(
            word(3, 0, "1:1:1", "ﱁ"),
            word(3, 1, "1:1:2", "ﱂ"),
        )

        val page = MushafMapper.toDomain(1, lines, words)

        val surah = page.lines.first { it.type == LineType.SURAH_NAME }
        val basmallah = page.lines.first { it.type == LineType.BASMALLAH }
        assertTrue(surah.words.isEmpty())
        assertEquals(1, surah.surahNumber)
        assertTrue(basmallah.words.isEmpty())
        assertEquals(2, page.lines.first { it.type == LineType.AYAH }.words.size)
    }

    @Test
    fun `lines are ordered by line number`() {
        val lines = listOf(
            line(3, "ayah"),
            line(1, "surah_name", centered = 1, surah = 1),
            line(2, "ayah"),
        )

        val page = MushafMapper.toDomain(1, lines, emptyList())

        assertEquals(listOf(1, 2, 3), page.lines.map { it.lineNumber })
    }
}
