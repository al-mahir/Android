package com.example.mushaf.presentation

import com.example.mushaf.presentation.components.MushafLayoutMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class MushafLayoutMathTest {

    private val refSp = MushafLayoutMath.REF_SP

     
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
        
        assertWidestFits(lineWidthsPx = listOf(2400, 2600, 2500), maxWidthPx = 1000, heightLimitSp = 40f)
    }

    @Test
    fun `height-bound page still keeps widest line within available width`() {
        
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
        
        val size = MushafLayoutMath.fillLineSizeSp(lineWidthPx = 700, maxWidthPx = 1000, heightLimitSp = 60f)
        val rendered = 700 * (size / refSp)
        assertTrue("fills close to width", rendered >= 1000 * 0.9f)
        assertTrue("never exceeds width", rendered <= 1000)
    }

    @Test
    fun `fillLineSizeSp is capped by the height limit`() {
        
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

    @Test
    fun `justified line pins first token to right edge and last to left edge`() {
        
        val widths = listOf(100f, 100f, 100f)
        val lefts = MushafLayoutMath.tokenLefts(widths, maxWidthPx = 600, centered = false)

        
        assertEquals(600f, lefts[0] + widths[0], 0.001f)
        
        assertEquals(0f, lefts[2], 0.001f)
        
        assertEquals(500f, lefts[0], 0.001f)
        assertEquals(250f, lefts[1], 0.001f)
    }

    @Test
    fun `justified tokens never overlap and stay in right-to-left order`() {
        val widths = listOf(80f, 120f, 60f, 140f)
        val lefts = MushafLayoutMath.tokenLefts(widths, maxWidthPx = 1000, centered = false)
        
        for (i in 1 until widths.size) {
            val prevLeft = lefts[i - 1]
            val thisRight = lefts[i] + widths[i]
            assertTrue("token $i must sit left of token ${i - 1}", thisRight <= prevLeft + 0.001f)
        }
    }

    @Test
    fun `centered group is packed adjacent and centred with equal margins`() {
        val widths = listOf(100f, 100f)
        val lefts = MushafLayoutMath.tokenLefts(widths, maxWidthPx = 600, centered = true)
        
        assertEquals(400f, lefts[0] + widths[0], 0.001f) 
        assertEquals(200f, lefts[1], 0.001f)             
        
        assertEquals(lefts[0], lefts[1] + widths[1], 0.001f)
    }

    @Test
    fun `single token is centred`() {
        val lefts = MushafLayoutMath.tokenLefts(listOf(200f), maxWidthPx = 600, centered = false)
        assertEquals(200f, lefts[0], 0.001f) 
    }

    @Test
    fun `empty line yields no positions`() {
        assertTrue(MushafLayoutMath.tokenLefts(emptyList(), maxWidthPx = 600, centered = false).isEmpty())
    }
}
