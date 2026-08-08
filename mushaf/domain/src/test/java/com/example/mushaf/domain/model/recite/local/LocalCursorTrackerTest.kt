package com.example.mushaf.domain.model.recite.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalCursorTrackerTest {

    private val window = listOf(
        LocalWordEntry("1:1:1", "بسم"),
        LocalWordEntry("1:1:2", "الله"),
        LocalWordEntry("1:1:3", "الرحمن"),
        LocalWordEntry("1:1:4", "الرحيم"),
    )

    @Test
    fun `an empty tracker has no current word`() {
        val tracker = LocalCursorTracker()
        assertNull(tracker.currentWordId)
    }

    @Test
    fun `setWindow anchors the bias index at the given word`() {
        val tracker = LocalCursorTracker()
        tracker.setWindow(window, anchorWordId = "1:1:2")
        assertEquals("1:1:2", tracker.currentWordId)
    }

    @Test
    fun `setWindow falls back to the first entry when the anchor is not in the window`() {
        val tracker = LocalCursorTracker()
        tracker.setWindow(window, anchorWordId = "9:9:9")
        assertEquals("1:1:1", tracker.currentWordId)
    }

    @Test
    fun `a matched word advances the bias index and returns its word id`() {
        val tracker = LocalCursorTracker()
        tracker.setWindow(window, anchorWordId = "1:1:1")

        assertEquals("1:1:2", tracker.offer("الله"))
        assertEquals("1:1:2", tracker.currentWordId)
    }

    @Test
    fun `an unmatched word is a no-op, never a guess`() {
        val tracker = LocalCursorTracker()
        tracker.setWindow(window, anchorWordId = "1:1:1")

        assertNull(tracker.offer("قلم"))
        assertEquals("1:1:1", tracker.currentWordId)
    }

    @Test
    fun `strict matching rejects a loose-only match that would otherwise resolve`() {
        val tracker = LocalCursorTracker()
        tracker.setWindow(window, anchorWordId = "1:1:1")

        // "بسم" only matches the shorter "بس" via the loose partial-containment fallback -
        // offer() must reject it since it always resolves strictly.
        assertNull(tracker.offer("بس"))
        assertEquals("1:1:1", tracker.currentWordId)
    }

    @Test
    fun `reset clears the window and bias index`() {
        val tracker = LocalCursorTracker()
        tracker.setWindow(window, anchorWordId = "1:1:2")

        tracker.reset()

        assertNull(tracker.currentWordId)
        assertNull(tracker.offer("بسم"))
    }
}
