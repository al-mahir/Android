package com.example.mushaf.presentation

import com.example.mushaf.presentation.font.PageFontProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test




 
class SurahNameMappingTest {

    private fun cp(surah: Int): Int = PageFontProvider.SURAH_NAME_CODEPOINTS[surah - 1]

    @Test
    fun `verified anchor surahs map to the expected codepoints`() {
        assertEquals(0xE904, cp(1))   
        assertEquals(0xE905, cp(2))   
        assertEquals(0xE938, cp(54))  
        assertEquals(0xE900, cp(59))  
        assertEquals(0xE901, cp(60))  
        assertEquals(0xE972, cp(114)) 
    }

    @Test
    fun `all 114 surahs map to distinct codepoints`() {
        assertEquals(114, PageFontProvider.SURAH_NAME_CODEPOINTS.size)
        assertEquals(114, PageFontProvider.SURAH_NAME_CODEPOINTS.toSet().size)
    }

    @Test
    fun `glyph lookup returns a string for valid surahs and null out of range`() {
        assertEquals(String(Character.toChars(0xE904)), PageFontProvider.surahNameGlyph(1))
        assertNull(PageFontProvider.surahNameGlyph(0))
        assertNull(PageFontProvider.surahNameGlyph(115))
    }
}
