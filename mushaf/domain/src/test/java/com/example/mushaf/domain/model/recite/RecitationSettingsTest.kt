package com.example.mushaf.domain.model.recite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test



 
class RecitationSettingsTest {

    @Test
    fun `grading everything sends no rules array at all`() {
        val settings = RecitationSettings(tajweedGradingEnabled = true, gradedRules = null)

        
        assertNull(settings.wireRules)
    }

    @Test
    fun `hifz-only sends an empty array, never null`() {
        val settings = RecitationSettings(tajweedGradingEnabled = false, gradedRules = null)

        assertEquals(emptySet<String>(), settings.wireRules)
    }

    @Test
    fun `turning tajwid grading off preserves the rule selection for when it comes back`() {
        val chosen = setOf("aared_madd", "ghonna")
        val off = RecitationSettings(tajweedGradingEnabled = false, gradedRules = chosen)

        assertEquals(emptySet<String>(), off.wireRules)
        assertEquals(chosen, off.copy(tajweedGradingEnabled = true).wireRules)
    }

    @Test
    fun `an untouched moshaf is omitted so the server's own defaults apply`() {
        val config = RecitationSettings().toConfig(from = null)

        // The schema's defaults disagree with what the server grades against on three madd
        
        
        assertTrue(config.moshaf.isEmpty())
    }

    @Test
    fun `the follow-along engine cannot grade tajwid`() {
        assertFalse(RecitationSettings(engine = "zipformer").engineCanGradeTajweed)
        assertTrue(RecitationSettings(engine = "real").engineCanGradeTajweed)
    }

    @Test
    fun `no stored engine means the server default, which does correct`() {
        assertTrue(RecitationSettings(engine = null).engineCanGradeTajweed)
    }

    @Test
    fun `settings reach the session config intact`() {
        val cursor = RecitationCursor(sura = 2, aya = 255, wordIndex = 0)
        val config = RecitationSettings(
            engine = "real",
            strictness = RecitationStrictness.LENIENT,
            gradedRules = setOf("qalqalah"),
            moshaf = mapOf("madd_mottasel_len" to MoshafValue.Number(6)),
        ).toConfig(cursor)

        assertEquals(cursor, config.start)
        assertEquals("real", config.engine)
        assertEquals(RecitationStrictness.LENIENT, config.strictness)
        assertEquals(setOf("qalqalah"), config.gradedRules)
        assertEquals(mapOf("madd_mottasel_len" to MoshafValue.Number(6)), config.moshaf)
    }
}
