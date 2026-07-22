package com.example.mushaf.domain

import com.example.mushaf.domain.model.recite.RecitationPacer
import com.example.mushaf.domain.model.recite.RecitationPacerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test






 
class RecitationPacerTest {

    private fun words(count: Int) = List(count) { "1:1:${it + 1}" }

    private fun pacerOn(
        wordCount: Int = 10,
        config: RecitationPacerConfig = RecitationPacerConfig(),
    ) = RecitationPacer(config).apply { setWords(words(wordCount)) }

    private fun RecitationPacer.speak(frames: Int) = repeat(frames) { onSpeechFrame() }

    @Test
    fun `nothing is highlighted before the reciter says anything`() {
        assertNull(pacerOn().currentWordId)
    }

    @Test
    fun `the first frame of speech lands on the first word`() {
        
        
        val pacer = pacerOn()

        assertEquals("1:1:1", pacer.onSpeechFrame())
    }

    @Test
    fun `the cursor advances as speech continues`() {
        val pacer = pacerOn(config = RecitationPacerConfig(initialFramesPerWord = 2f))

        pacer.speak(1)
        assertEquals("1:1:1", pacer.currentWordId)
        pacer.speak(2)
        assertEquals("1:1:2", pacer.currentWordId)
        pacer.speak(2)
        assertEquals("1:1:3", pacer.currentWordId)
    }

    @Test
    fun `silence holds the cursor in place`() {
        
        
        val pacer = pacerOn(config = RecitationPacerConfig(initialFramesPerWord = 2f))
        pacer.speak(3)
        val whereTheyStopped = pacer.currentWordId

        repeat(100) {   }

        assertEquals(whereTheyStopped, pacer.currentWordId)
    }

    @Test
    fun `the estimate never runs far ahead of confirmed ground truth`() {
        
        
        val config = RecitationPacerConfig(initialFramesPerWord = 1f, maxLookaheadWords = 3)
        val pacer = pacerOn(wordCount = 50, config = config)

        pacer.speak(200)

        assertTrue("the cursor ran past its lookahead limit", pacer.isAtLookaheadLimit)
        
        
        
        assertEquals("1:1:3", pacer.currentWordId)
    }

    @Test
    fun `a confirmed cursor releases the lookahead limit`() {
        val config = RecitationPacerConfig(initialFramesPerWord = 1f, maxLookaheadWords = 3)
        val pacer = pacerOn(wordCount = 50, config = config)
        pacer.speak(200)

        pacer.confirm("1:1:4")
        pacer.speak(3)

        assertEquals("1:1:7", pacer.currentWordId)
    }

    @Test
    fun `ground truth pulls the cursor back when the guess ran ahead`() {
        val pacer = pacerOn(config = RecitationPacerConfig(initialFramesPerWord = 1f))
        pacer.speak(5)

        pacer.confirm("1:1:2")

        assertEquals("1:1:2", pacer.currentWordId)
    }

    @Test
    fun `ground truth pushes the cursor forward when the reciter was quicker`() {
        val pacer = pacerOn(config = RecitationPacerConfig(initialFramesPerWord = 20f))
        pacer.speak(5)

        pacer.confirm("1:1:6")

        assertEquals("1:1:6", pacer.currentWordId)
    }

    @Test
    fun `a confirmation for a word not on this page is ignored`() {
        val pacer = pacerOn()
        pacer.speak(1)

        pacer.confirm("2:255:1")

        assertEquals("1:1:1", pacer.currentWordId)
    }

    @Test
    fun `the pacer learns the reciter's own speed`() {
        // Mujawwad and hadr differ several-fold, so a fixed rate is visibly wrong for almost
        // everyone. Four words over 40 frames is 10 frames per word.
        val pacer = pacerOn(config = RecitationPacerConfig(initialFramesPerWord = 5f, paceSmoothing = 1f))

        pacer.observePace(wordCount = 4, speechFrames = 40)

        assertEquals(10f, pacer.estimatedFramesPerWord, 0.001f)
    }

    @Test
    fun `a nonsense pace measurement cannot poison the estimate`() {
        val config = RecitationPacerConfig(paceSmoothing = 1f, minFramesPerWord = 1.5f, maxFramesPerWord = 20f)
        val pacer = pacerOn(config = config)

        pacer.observePace(wordCount = 1, speechFrames = 5_000)
        assertEquals(20f, pacer.estimatedFramesPerWord, 0.001f)

        pacer.observePace(wordCount = 500, speechFrames = 1)
        assertEquals(1.5f, pacer.estimatedFramesPerWord, 0.001f)
    }

    @Test
    fun `an empty or zero measurement is ignored`() {
        val pacer = pacerOn(config = RecitationPacerConfig(initialFramesPerWord = 5f))

        pacer.observePace(wordCount = 0, speechFrames = 30)
        pacer.observePace(wordCount = 3, speechFrames = 0)

        assertEquals(5f, pacer.estimatedFramesPerWord, 0.001f)
    }

    @Test
    fun `the cursor stops at the last word rather than running off the page`() {
        val pacer = pacerOn(wordCount = 3, config = RecitationPacerConfig(initialFramesPerWord = 1f))

        pacer.speak(50)

        assertEquals("1:1:3", pacer.currentWordId)
        assertTrue(pacer.isAtEndOfWords)
    }

    @Test
    fun `re-supplying the same page keeps the reciter's place`() {
        
        val pacer = pacerOn(config = RecitationPacerConfig(initialFramesPerWord = 1f))
        pacer.speak(4)
        val place = pacer.currentWordId

        pacer.setWords(words(10))

        assertEquals(place, pacer.currentWordId)
    }

    @Test
    fun `turning to a page that does not contain the current word starts it fresh`() {
        val pacer = pacerOn(config = RecitationPacerConfig(initialFramesPerWord = 1f))
        pacer.speak(4)

        pacer.setWords(listOf("2:1:1", "2:1:2"))

        assertNull(pacer.currentWordId)
        assertEquals("2:1:1", pacer.onSpeechFrame())
    }

    @Test
    fun `reset clears both the position and the learned pace`() {
        val pacer = pacerOn()
        pacer.speak(10)
        pacer.observePace(wordCount = 2, speechFrames = 40)

        pacer.reset()

        assertNull(pacer.currentWordId)
        assertEquals(RecitationPacerConfig().initialFramesPerWord, pacer.estimatedFramesPerWord, 0.001f)
    }

    @Test
    fun `placeAtStart shows a position before a word is spoken`() {
        
        
        val pacer = pacerOn()

        pacer.placeAtStart()

        assertEquals("1:1:1", pacer.currentWordId)
    }

    @Test
    fun `placeAtStart confirms nothing, so the lookahead budget is intact`() {
        val config = RecitationPacerConfig(initialFramesPerWord = 1f, maxLookaheadWords = 3)
        val pacer = pacerOn(wordCount = 50, config = config)

        pacer.placeAtStart()
        pacer.speak(100)

        
        assertEquals("1:1:3", pacer.currentWordId)
    }

    @Test
    fun `placeAtStart on an empty page does nothing`() {
        val pacer = RecitationPacer()

        pacer.placeAtStart()

        assertNull(pacer.currentWordId)
    }

    @Test
    fun `the cursor tracks a whole ayah without stalling part-way through`() {
        
        
        
        val pacer = pacerOn(
            wordCount = 40,
            config = RecitationPacerConfig(initialFramesPerWord = 1f),
        )

        pacer.speak(18)

        assertFalse(
            "the cursor stalled before covering a typical āyah",
            pacer.isAtLookaheadLimit,
        )
    }

    @Test
    fun `the cursor eases off as it runs ahead rather than stopping dead`() {
        val config = RecitationPacerConfig(
            initialFramesPerWord = 1f,
            maxLookaheadWords = 20,
            easeAfterWords = 10,
            maxDrag = 2.5f,
        )
        val pacer = pacerOn(wordCount = 60, config = config)

        pacer.speak(10)
        val atThreshold = pacer.currentWordId
        pacer.speak(10)
        val afterThreshold = pacer.currentWordId

        
        
        assertEquals("1:1:10", atThreshold)
        assertNotEquals("the cursor stopped instead of easing", atThreshold, afterThreshold)
        assertNotEquals(
            "the cursor did not slow down past the ease threshold",
            "1:1:20",
            afterThreshold,
        )
    }

    @Test
    fun `easing never carries the cursor past the hard bound`() {
        val config = RecitationPacerConfig(
            initialFramesPerWord = 1f,
            maxLookaheadWords = 12,
            easeAfterWords = 4,
        )
        val pacer = pacerOn(wordCount = 80, config = config)

        pacer.speak(500)

        
        
        assertTrue(pacer.isAtLookaheadLimit)
        assertEquals("1:1:12", pacer.currentWordId)
    }

    @Test
    fun `an ease threshold at or past the bound behaves as a plain cap`() {
        val config = RecitationPacerConfig(
            initialFramesPerWord = 1f,
            maxLookaheadWords = 3,
            easeAfterWords = 10,
        )
        val pacer = pacerOn(wordCount = 50, config = config)

        pacer.speak(200)

        assertEquals("1:1:3", pacer.currentWordId)
    }
}
