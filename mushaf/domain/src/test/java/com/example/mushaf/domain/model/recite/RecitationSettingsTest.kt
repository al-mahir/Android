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
    fun `nothing stored still names an engine, chosen by the practice mode`() {
        // Leaving this null was letting the server pick its own default for both practice modes.
        assertEquals("real", RecitationSettings(tajweedGradingEnabled = true).wireEngine)
        assertEquals("zipformer", RecitationSettings(tajweedGradingEnabled = false).wireEngine)
        assertEquals("real", RecitationSettings().toConfig(from = null).engine)
    }

    @Test
    fun `switching practice mode switches the engine with it`() {
        val hifzOnly = RecitationSettings().withTajweedGrading(false)
        assertEquals("zipformer", hifzOnly.engine)
        assertFalse(hifzOnly.gradesTajweed)

        val withTajweed = hifzOnly.withTajweedGrading(true)
        assertEquals("real", withTajweed.engine)
        assertTrue(withTajweed.gradesTajweed)
    }

    @Test
    fun `switching engine switches the practice mode with it`() {
        assertFalse(RecitationSettings().withEngine("zipformer").gradesTajweed)
        assertTrue(RecitationSettings().withEngine("real").gradesTajweed)
    }

    @Test
    fun `an engine that cannot grade tajwid overrules a stale grading flag`() {
        // Settings persisted before the two were written together can hold this pair.
        val stale = RecitationSettings(engine = "zipformer", tajweedGradingEnabled = true)

        assertFalse("the engine has the last word", stale.gradesTajweed)
        assertEquals(emptySet<String>(), stale.wireRules)
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
