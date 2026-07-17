package com.example.mushaf.presentation

import com.example.mushaf.presentation.components.MushafLayoutMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class MushafLayoutMathTest {

    private val refSp = MushafLayoutMath.REF_SP

    /** Scale a REF_SP width to the chosen size and assert it fits the page width. */
    private fun assertWidestFits(lineWidthsPx: List<Int>, maxWidthPx: Int, heightLimitSp: Float) {
        val sizeSp = MushafLayoutMath.uniformAyahSizeSp(lineWidthsPx, maxWidthPx, heightLimitSp)
        val widestAtRef = lineWidthsPx.max()
        val widestAtSize = widestAtRef * (sizeSp / refSp)
        assertTrue(
            "widest line ($widestAtSize px) must fit page width ($maxWidthPx px) at $sizeSp sp",
            widestAtSize <= maxWidthPx,
        )
    }

    @Test
    fun `width-bound page keeps widest line within available width`() {
        // Wide lines with generous height budget: width is the binding constraint.
        assertWidestFits(lineWidthsPx = listOf(2400, 2600, 2500), maxWidthPx = 1000, heightLimitSp = 40f)
    }

    @Test
    fun `height-bound page still keeps widest line within available width`() {
        // Small height budget binds; width must remain safe too.
        assertWidestFits(lineWidthsPx = listOf(600, 700, 650), maxWidthPx = 1000, heightLimitSp = 18f)
    }

    @Test
    fun `size never exceeds the max cap`() {
        val size = MushafLayoutMath.uniformAyahSizeSp(listOf(100), maxWidthPx = 4000, heightLimitSp = 4000f)
        assertEquals(MushafLayoutMath.MAX_SP, size, 0.001f)
    }

    @Test
    fun `empty or degenerate input falls back to max size instead of crashing`() {
        assertEquals(MushafLayoutMath.MAX_SP, MushafLayoutMath.uniformAyahSizeSp(emptyList(), 1000, 40f), 0.001f)
        assertEquals(MushafLayoutMath.MAX_SP, MushafLayoutMath.uniformAyahSizeSp(listOf(500), 0, 40f), 0.001f)
    }

    @Test
    fun `fillLineSizeSp makes a line fill the width without exceeding it`() {
        // A narrow line is scaled UP to fill, but never past the page width.
        val size = MushafLayoutMath.fillLineSizeSp(lineWidthPx = 700, maxWidthPx = 1000, heightLimitSp = 60f)
        val rendered = 700 * (size / refSp)
        assertTrue("fills close to width", rendered >= 1000 * 0.9f)
        assertTrue("never exceeds width", rendered <= 1000)
    }

    @Test
    fun `fillLineSizeSp is capped by the height limit`() {
        // Very narrow line would need a huge size to fill; the slot height caps it.
        val size = MushafLayoutMath.fillLineSizeSp(lineWidthPx = 200, maxWidthPx = 1000, heightLimitSp = 22f)
        assertEquals(22f, size, 0.001f)
    }

    @Test
    fun `fitSizeSp keeps content within both width and height bounds`() {
        val size = MushafLayoutMath.fitSizeSp(contentWidthPx = 2000, contentHeightPx = 80, maxWidthPx = 1000, maxHeightPx = 120f)
        val scaledWidth = 2000 * (size / refSp)
        val scaledHeight = 80 * (size / refSp)
        assertTrue("width fits", scaledWidth <= 1000)
        assertTrue("height fits", scaledHeight <= 120f)
    }
}
