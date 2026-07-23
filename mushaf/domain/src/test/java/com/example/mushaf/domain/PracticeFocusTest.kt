package com.example.mushaf.domain

import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMistake
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.RecitationWordStatus
import com.example.mushaf.domain.model.recite.SpeechErrorType
import com.example.mushaf.domain.model.recite.TajweedRuleReference
import com.example.mushaf.domain.model.recite.practiceFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeFocusTest {

    private var nextIndex = 0

    private fun word(
        rule: String? = null,
        category: MistakeCategory = MistakeCategory.TAJWID,
        status: RecitationWordStatus = RecitationWordStatus.ERROR,
        trimmed: Boolean = false,
    ) = RecitationWordFeedback(
        position = RecitationCursor(1, 1, nextIndex++),
        uthmani = "w",
        status = status,
        mistakes = listOf(
            RecitationMistake(
                category = category,
                rawChannel = "tajweed",
                speechErrorType = SpeechErrorType.REPLACE,
                uthmaniSpan = null,
                expectedPhonemes = null,
                predictedPhonemes = null,
                expectedLength = null,
                actualLength = null,
                rules = listOfNotNull(rule?.let { TajweedRuleReference(it, null, null, null, null) }),
                confidence = 0.9f,
            ),
        ),
        isTrimmed = trimmed,
    )

    @Test
    fun `a single slip is not a pattern`() {
        
        val focus = listOf(word(rule = "المد الطبيعي")).practiceFocus()

        assertTrue(focus.isEmpty())
    }

    @Test
    fun `a repeated rule becomes the thing to practise`() {
        val focus = listOf(
            word(rule = "المد الطبيعي"),
            word(rule = "المد الطبيعي"),
            word(rule = "المد الطبيعي"),
        ).practiceFocus()

        assertEquals(1, focus.size)
        assertEquals("المد الطبيعي", focus.single().ruleName)
        assertEquals(3, focus.single().occurrences)
    }

    @Test
    fun `distinct rules stay distinct rather than collapsing into their channel`() {
        
        val focus = listOf(
            word(rule = "المد الطبيعي"), word(rule = "المد الطبيعي"),
            word(rule = "الغنة"), word(rule = "الغنة"),
        ).practiceFocus()

        assertEquals(2, focus.size)
        assertEquals(setOf("المد الطبيعي", "الغنة"), focus.map { it.ruleName }.toSet())
    }

    @Test
    fun `the most frequent pattern comes first`() {
        val focus = listOf(
            word(rule = "الغنة"), word(rule = "الغنة"),
            word(rule = "المد الطبيعي"), word(rule = "المد الطبيعي"), word(rule = "المد الطبيعي"),
        ).practiceFocus()

        assertEquals("المد الطبيعي", focus.first().ruleName)
        assertEquals(3, focus.first().occurrences)
    }

    @Test
    fun `hints never become a diagnosis`() {
        
        
        val focus = List(5) { word(rule = "المد الطبيعي", status = RecitationWordStatus.ALMOST) }
            .practiceFocus()

        assertTrue("softened findings were turned into a lesson", focus.isEmpty())
    }

    @Test
    fun `unverified words never become a diagnosis`() {
        val focus = List(5) { word(rule = "المد الطبيعي", trimmed = true) }.practiceFocus()

        assertTrue("unscored words were turned into a lesson", focus.isEmpty())
    }

    @Test
    fun `findings without a rule still group by their channel`() {
        val focus = listOf(
            word(rule = null, category = MistakeCategory.TASHKIL),
            word(rule = null, category = MistakeCategory.TASHKIL),
        ).practiceFocus()

        assertEquals(MistakeCategory.TASHKIL, focus.single().category)
        assertEquals(null, focus.single().ruleName)
        assertEquals(2, focus.single().occurrences)
    }

    @Test
    fun `the list is capped so it stays a lesson rather than a second transcript`() {
        val focus = listOf(
            word(rule = "a"), word(rule = "a"), word(rule = "a"), word(rule = "a"),
            word(rule = "b"), word(rule = "b"), word(rule = "b"),
            word(rule = "c"), word(rule = "c"),
            word(rule = "d"), word(rule = "d"),
        ).practiceFocus()

        assertEquals(3, focus.size)
        assertEquals(listOf("a", "b", "c"), focus.map { it.ruleName })
    }

    @Test
    fun `a clean recitation has nothing to practise`() {
        val focus = listOf(
            RecitationWordFeedback(
                position = RecitationCursor(1, 1, 0),
                uthmani = "w",
                status = RecitationWordStatus.CORRECT,
                mistakes = emptyList(),
                isTrimmed = false,
            ),
        ).practiceFocus()

        assertTrue(focus.isEmpty())
    }
}
