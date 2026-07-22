package com.example.mushaf.domain

import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMistake
import com.example.mushaf.domain.model.recite.RecitationSessionRecorder
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.RecitationWordStatus
import com.example.mushaf.domain.model.recite.SpeechErrorType
import com.example.mushaf.domain.model.recite.TajweedRuleReference
import com.iti.domain.model.recitation.SessionMistakeCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test






 
class RecitationSessionRecorderTest {

    private fun mistake(
        category: MistakeCategory = MistakeCategory.TAJWID,
        rule: String? = null,
        expected: Int? = null,
        actual: Int? = null,
    ) = RecitationMistake(
        category = category,
        rawChannel = "tajweed",
        speechErrorType = SpeechErrorType.REPLACE,
        uthmaniSpan = null,
        expectedPhonemes = null,
        predictedPhonemes = null,
        expectedLength = expected,
        actualLength = actual,
        rules = listOfNotNull(rule?.let { TajweedRuleReference(it, null, null, null, null) }),
        confidence = 0.9f,
    )

    private fun word(
        sura: Int = 1,
        aya: Int = 1,
        index: Int,
        text: String = "w$index",
        status: RecitationWordStatus = RecitationWordStatus.CORRECT,
        trimmed: Boolean = false,
        mistakes: List<RecitationMistake> = emptyList(),
    ) = RecitationWordFeedback(
        position = RecitationCursor(sura, aya, index),
        uthmani = text,
        status = status,
        mistakes = mistakes,
        isTrimmed = trimmed,
    )

    private fun record(vararg words: RecitationWordFeedback) = RecitationSessionRecorder.record(
        id = "session-1",
        startedAtEpochMs = 1_000L,
        durationMs = 60_000L,
        wordFeedback = words.associateBy { it.wordId },
        fallbackPosition = RecitationCursor(1, 1, 0),
    )

    @Test
    fun `only confident mistakes are written down`() {
        
        val summary = record(
            word(index = 0, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake())),
            word(index = 1, status = RecitationWordStatus.ALMOST, mistakes = listOf(mistake())),
            word(index = 2, status = RecitationWordStatus.ERROR, trimmed = true, mistakes = listOf(mistake())),
        )

        assertEquals(1, summary.mistakeCount)
        assertEquals(0, summary.mistakes.single().wordIndex)
    }

    @Test
    fun `unverified words are excluded from the denominator`() {
        
        val summary = record(
            word(index = 0),
            word(index = 1),
            word(index = 2, trimmed = true),
            word(index = 3, trimmed = true),
        )

        assertEquals(2, summary.scoredWordCount)
        assertEquals(1f, summary.accuracy!!, 0.0001f)
    }

    @Test
    fun `accuracy counts hints as not-mistakes`() {
        val summary = record(
            word(index = 0),
            word(index = 1, status = RecitationWordStatus.ALMOST, mistakes = listOf(mistake())),
            word(index = 2, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake())),
        )

        assertEquals(3, summary.scoredWordCount)
        assertEquals(1, summary.mistakeCount)
        assertEquals(2f / 3f, summary.accuracy!!, 0.0001f)
    }

    @Test
    fun `a session that graded nothing has no accuracy and says so`() {
        
        val summary = record()

        assertTrue(summary.gradedNothing)
        assertNull(summary.accuracy)
        assertEquals(0, summary.mistakeCount)
    }

    @Test
    fun `the range spans the first and last graded word`() {
        val summary = record(
            word(sura = 1, aya = 7, index = 0),
            word(sura = 1, aya = 3, index = 0),
        )

        assertEquals(3, summary.start.aya)
        assertEquals(7, summary.end.aya)
    }

    @Test
    fun `the range falls back to the cursor when nothing was graded`() {
        val summary = RecitationSessionRecorder.record(
            id = "s",
            startedAtEpochMs = 0,
            durationMs = 0,
            wordFeedback = emptyMap(),
            fallbackPosition = RecitationCursor(2, 255, 0),
        )

        assertEquals(2, summary.start.sura)
        assertEquals(255, summary.start.aya)
    }

    @Test
    fun `the live taxonomy crosses cleanly into the recorded one`() {
        fun categoryOf(category: MistakeCategory) = record(
            word(index = 0, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake(category))),
        ).mistakes.single().category

        assertEquals(SessionMistakeCategory.MEMORIZATION, categoryOf(MistakeCategory.MEMORIZATION))
        assertEquals(SessionMistakeCategory.TASHKIL, categoryOf(MistakeCategory.TASHKIL))
        assertEquals(SessionMistakeCategory.TAJWID, categoryOf(MistakeCategory.TAJWID))
        assertEquals(SessionMistakeCategory.OTHER, categoryOf(MistakeCategory.OTHER))
    }

    @Test
    fun `a rule and its lengths survive into the record`() {
        val summary = record(
            word(
                index = 0,
                text = "ٱلرَّحْمَٰنِ",
                status = RecitationWordStatus.ERROR,
                mistakes = listOf(mistake(rule = "المد الطبيعي", expected = 2, actual = 3)),
            ),
        )

        val stored = summary.mistakes.single()
        assertEquals("ٱلرَّحْمَٰنِ", stored.word)
        assertEquals("المد الطبيعي", stored.ruleName)
        assertEquals(2, stored.expectedLength)
        assertEquals(3, stored.actualLength)
    }

    @Test
    fun `recurring findings are recorded as something to practise`() {
        val summary = record(
            word(index = 0, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake(rule = "المد الطبيعي"))),
            word(index = 1, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake(rule = "المد الطبيعي"))),
            word(index = 2, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake(rule = "الغنة"))),
        )

        assertEquals("المد الطبيعي", summary.practiceFocus.single().ruleName)
        assertEquals(2, summary.practiceFocus.single().occurrences)
    }

    @Test
    fun `the breakdown groups mistakes by channel`() {
        val summary = record(
            word(index = 0, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake(MistakeCategory.TAJWID))),
            word(index = 1, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake(MistakeCategory.TAJWID))),
            word(index = 2, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake(MistakeCategory.TASHKIL))),
        )

        assertEquals(2, summary.mistakesByCategory[SessionMistakeCategory.TAJWID])
        assertEquals(1, summary.mistakesByCategory[SessionMistakeCategory.TASHKIL])
    }
}
