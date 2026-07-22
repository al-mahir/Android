package com.example.mushaf.presentation

import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.RecitationWordStatus
import com.example.mushaf.presentation.state.LiveCorrectionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The accuracy figure the reader shows.
 *
 * It is session feedback, not an assessment — the thresholds behind `almost` versus `error` are
 * documented as uncalibrated — so the rules here are about what it must never overstate.
 */
class LiveCorrectionAccuracyTest {

    private fun word(
        index: Int,
        status: RecitationWordStatus,
        trimmed: Boolean = false,
    ) = RecitationWordFeedback(
        position = RecitationCursor(1, 1, index),
        uthmani = "w$index",
        status = status,
        mistakes = emptyList(),
        isTrimmed = trimmed,
    )

    private fun stateOf(vararg words: RecitationWordFeedback) =
        LiveCorrectionUiState(wordFeedback = words.associateBy { it.wordId })

    @Test
    fun `no scored words yields no figure at all`() {
        // Showing 100% before anything was checked would assert something about a recitation
        // nobody graded.
        assertNull(LiveCorrectionUiState().accuracy)
    }

    @Test
    fun `a clean recitation is full accuracy`() {
        val state = stateOf(
            word(0, RecitationWordStatus.CORRECT),
            word(1, RecitationWordStatus.CORRECT),
        )

        assertEquals(1f, state.accuracy!!, 0.0001f)
        assertEquals(2, state.scoredWordCount)
    }

    @Test
    fun `each confident mistake lowers it`() {
        val state = stateOf(
            word(0, RecitationWordStatus.CORRECT),
            word(1, RecitationWordStatus.CORRECT),
            word(2, RecitationWordStatus.CORRECT),
            word(3, RecitationWordStatus.ERROR),
        )

        assertEquals(0.75f, state.accuracy!!, 0.0001f)
    }

    @Test
    fun `a hint does not lower it`() {
        // The service softened that finding because it was not confident enough to accuse.
        // Counting it here would re-harden it into a penalty and undo the safety property.
        val state = stateOf(
            word(0, RecitationWordStatus.CORRECT),
            word(1, RecitationWordStatus.ALMOST),
        )

        assertEquals(1f, state.accuracy!!, 0.0001f)
        assertEquals(0, state.mistakeCount)
    }

    @Test
    fun `unverified words are excluded from both halves of the fraction`() {
        // A word the chunker cut was never checked. Counting it as a success would inflate the
        // figure; counting it as a failure would penalise a recitation nobody graded.
        val state = stateOf(
            word(0, RecitationWordStatus.CORRECT),
            word(1, RecitationWordStatus.ERROR),
            word(2, RecitationWordStatus.CORRECT, trimmed = true),
            word(3, RecitationWordStatus.ERROR, trimmed = true),
        )

        assertEquals("unscored words were counted", 2, state.scoredWordCount)
        assertEquals(0.5f, state.accuracy!!, 0.0001f)
    }

    @Test
    fun `an all-unverified session yields no figure`() {
        val state = stateOf(
            word(0, RecitationWordStatus.CORRECT, trimmed = true),
            word(1, RecitationWordStatus.CORRECT, trimmed = true),
        )

        assertEquals(0, state.scoredWordCount)
        assertNull(state.accuracy)
    }

    @Test
    fun `every word wrong is zero, not a division error`() {
        val state = stateOf(
            word(0, RecitationWordStatus.ERROR),
            word(1, RecitationWordStatus.ERROR),
        )

        assertEquals(0f, state.accuracy!!, 0.0001f)
    }
}
