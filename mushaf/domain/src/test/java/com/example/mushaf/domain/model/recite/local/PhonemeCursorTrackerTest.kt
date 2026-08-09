package com.example.mushaf.domain.model.recite.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhonemeCursorTrackerTest {

    /** Al-Fātiḥa 1:1, straight out of the reference table. */
    private val fatihaUnits = listOf(
        ReferencePhonemeUnit("بِسمِ", listOf("1:1:1")),
        ReferencePhonemeUnit("للَااهِ", listOf("1:1:2")),
        ReferencePhonemeUnit("ررَحمَاانِ", listOf("1:1:3")),
        ReferencePhonemeUnit("ررَحِۦۦۦۦم", listOf("1:1:4")),
    )

    private fun trackerOn(units: List<ReferencePhonemeUnit>) =
        PhonemeCursorTracker().apply { setWindow(units) }

    @Test
    fun `a growing transcript walks the window one word at a time`() {
        val tracker = trackerOn(fatihaUnits)

        assertEquals("1:1:1", tracker.offer("بِسمِ"))
        assertEquals("1:1:2", tracker.offer("بِسمِ للَااهِ"))
        assertEquals("1:1:3", tracker.offer("بِسمِ للَااهِ ررَحمَاانِ"))
    }

    @Test
    fun `mid-word decodes report nothing rather than guessing the word being said`() {
        val tracker = trackerOn(fatihaUnits)

        assertNull("half a word is not a word", tracker.offer("بِس"))
        assertEquals("1:1:1", tracker.offer("بِسمِ"))
    }

    @Test
    fun `catching up several words at once reports only the furthest`() {
        val tracker = trackerOn(fatihaUnits)

        // A decode that lands after a burst of speech - the caller only needs where the reciter
        // ended up, not every word crossed on the way.
        assertEquals("1:1:3", tracker.offer("بِسمِ للَااهِ ررَحمَاانِ"))
    }

    @Test
    fun `misheard phonemes still match, wrong words do not`() {
        val tolerant = trackerOn(fatihaUnits)
        assertEquals(
            "a single misheard letter must not stall the highlight",
            "1:1:1",
            tolerant.offer("بِسنِ"),
        )

        val strict = trackerOn(fatihaUnits)
        assertNull("an unrelated sound must never move the cursor", strict.offer("قَلَمُون"))
    }

    @Test
    fun `the cursor never leaves the window it was given`() {
        val tracker = trackerOn(fatihaUnits)

        val everything = "بِسمِ للَااهِ ررَحمَاانِ ررَحِۦۦۦۦم ءَلحَمدُ لِللَااهِ رَببِ"
        assertEquals("1:1:4", tracker.offer(everything))
        assertNull("nothing is left in the window to report", tracker.offer("$everything لعَاالَمِۦۦۦۦن"))
    }

    @Test
    fun `it never walks backwards through the window`() {
        val tracker = trackerOn(fatihaUnits)
        tracker.offer("بِسمِ للَااهِ")

        assertNull("a shrunken transcript must not rewind the cursor", tracker.offer("بِسمِ"))
        assertEquals("1:1:2", tracker.currentWordId)
    }

    @Test
    fun `re-anchoring writes off everything heard so far`() {
        val tracker = trackerOn(fatihaUnits)
        tracker.offer("بِسمِ للَااهِ")

        // What a server chunk does: a fresh window starting after the confirmed word. The
        // transcript still holds the phonemes of the words before it, and they must not be
        // replayed against the new window.
        tracker.setWindow(
            listOf(
                ReferencePhonemeUnit("ررَحمَاانِ", listOf("1:1:3")),
                ReferencePhonemeUnit("ررَحِۦۦۦۦم", listOf("1:1:4")),
            ),
        )

        assertNull(tracker.offer("بِسمِ للَااهِ"))
        assertEquals("1:1:3", tracker.offer("بِسمِ للَااهِ ررَحمَاانِ"))
    }

    @Test
    fun `a merged span reports its last word`() {
        val tracker = trackerOn(
            listOf(ReferencePhonemeUnit("هُدَللِلمُتتَقِۦۦۦۦن", listOf("2:2:6", "2:2:7"))),
        )

        assertEquals("2:2:7", tracker.offer("هُدَللِلمُتتَقِۦۦۦۦن"))
    }

    @Test
    fun `an empty window is inert`() {
        val tracker = PhonemeCursorTracker()

        assertNull(tracker.offer("بِسمِ للَااهِ"))
        assertNull(tracker.currentWordId)
    }

    @Test
    fun `reset clears everything`() {
        val tracker = trackerOn(fatihaUnits)
        tracker.offer("بِسمِ")

        tracker.reset()

        assertNull(tracker.currentWordId)
        assertNull(tracker.offer("للَااهِ"))
    }
}
