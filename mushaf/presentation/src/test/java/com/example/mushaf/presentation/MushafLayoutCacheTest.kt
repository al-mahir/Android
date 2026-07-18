package com.example.mushaf.presentation

import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.MushafLine
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.MushafWord
import com.example.mushaf.domain.model.ReadingMode
import com.example.mushaf.presentation.components.MushafLayoutCache
import com.example.mushaf.presentation.components.MushafLayoutMath
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class MushafLayoutCacheTest {

    private companion object {
        // MushafLayoutCache is a process-level singleton shared across every test method, and JUnit
        // builds a fresh test instance per method — so page numbers must be unique across the whole
        // class run, not per instance, or one test's cached entry would satisfy another's lookup.
        val pageSeq = AtomicInteger(1000)
    }

    private fun uniquePage(lines: List<MushafLine>) =
        MushafPage(pageNumber = pageSeq.getAndIncrement(), lines = lines)

    private fun word(line: Int, pos: Int, glyphs: String) =
        MushafWord(
            id = "$line-$pos",
            glyphs = glyphs,
            pageNumber = 0,
            lineNumber = line,
            positionInLine = pos,
            isEndOfAyah = false,
        )

    private fun ayah(line: Int, centered: Boolean, vararg words: String) =
        MushafLine(
            lineNumber = line,
            type = LineType.AYAH,
            isCentered = centered,
            surahNumber = null,
            words = words.mapIndexed { i, g -> word(line, i, g) },
        )

    // 100px per glyph char: keeps measured widths easy to reason about while staying large enough
    // that width (not the MAX_SP cap) is the binding constraint at the height limits used below.
    private val widthPerChar: (String) -> Int = { it.length * 100 }

    @Test
    fun `measures each ayah line once, not once per word`() {
        val page = uniquePage(
            listOf(
                ayah(1, centered = false, "aa", "bb", "cc"),
                ayah(2, centered = false, "dddd", "ee"),
            ),
        )
        var calls = 0
        MushafLayoutCache.lineSizes(page, ReadingMode.PLAIN, widthPx = 1000, heightLimitSp = 200f) {
            calls++
            widthPerChar(it)
        }
        // Two ayah lines -> two measure calls, even though there are five words total.
        assertEquals(2, calls)
    }

    @Test
    fun `line width is the concatenation of its words' glyphs`() {
        // Line 1 concatenates to 6 chars (600px), line 2 to 6 chars (600px): equal natural widths.
        val page = uniquePage(
            listOf(
                ayah(1, centered = false, "aa", "bb", "cc"),
                ayah(2, centered = false, "dddddd"),
            ),
        )
        val sizes = MushafLayoutCache.lineSizes(page, ReadingMode.PLAIN, widthPx = 1000, heightLimitSp = 200f, widthPerChar)
        // Line 1 is justified; its size is the fill for a 600px-wide line.
        assertEquals(
            MushafLayoutMath.fillLineSizeSp(600, 1000, 200f),
            sizes.getValue(1),
            0.001f,
        )
    }

    @Test
    fun `centered line uses the uniform base size, not a stretched fill`() {
        val page = uniquePage(
            listOf(
                ayah(1, centered = false, "wwwwwwwwww"), // wide justified reference (1000px)
                ayah(2, centered = true, "x"),            // short centered line (100px)
            ),
        )
        val sizes = MushafLayoutCache.lineSizes(page, ReadingMode.PLAIN, widthPx = 1000, heightLimitSp = 200f, widthPerChar)
        val base = MushafLayoutMath.uniformAyahSizeSp(listOf(1000, 100), 1000, 200f)
        assertEquals(base, sizes.getValue(2), 0.001f)
        // The short centered line is NOT blown up to fill the width on its own.
        assertTrue(sizes.getValue(2) < MushafLayoutMath.fillLineSizeSp(100, 1000, 200f))
    }

    @Test
    fun `repeated lookup with the same key does not re-measure`() {
        val page = uniquePage(listOf(ayah(1, centered = false, "aa", "bb")))
        var calls = 0
        val measure: (String) -> Int = { calls++; widthPerChar(it) }
        val first = MushafLayoutCache.lineSizes(page, ReadingMode.PLAIN, 1000, 200f, measure)
        val second = MushafLayoutCache.lineSizes(page, ReadingMode.PLAIN, 1000, 200f, measure)
        assertEquals(1, calls) // second lookup served from cache
        assertEquals(first, second)
    }
}
