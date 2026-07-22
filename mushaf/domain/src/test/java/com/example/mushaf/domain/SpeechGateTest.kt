package com.example.mushaf.domain

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import com.example.mushaf.domain.model.recite.SpeechGate
import com.example.mushaf.domain.model.recite.SpeechGateConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The speech gate decides what the reciter's microphone actually sends. Two failures matter, and
 * they pull in opposite directions:
 *
 * - Dropping speech turns a correct recitation into a reported "missing word".
 * - Dropping the silence *after* speech stops the server ever finalizing a chunk, so no feedback
 *   arrives at all (`docs/API.md` §5.3 — its VAD needs ≥300 ms of trailing silence).
 *
 * These tests pin both edges.
 */
class SpeechGateTest {

    private var nextSample = 0L

    /** A frame of room tone: audible, but far below speech. */
    private fun silence() = frameOf(amplitude = 30)

    /** A frame of speech-level audio. */
    private fun speech() = frameOf(amplitude = 8_000)

    private fun frameOf(amplitude: Int): AudioFrame {
        val frame = AudioFrame(
            samples = ShortArray(RecitationAudioFormat.FRAME_SAMPLES) { index ->
                // Alternate sign so the frame has energy without a DC offset.
                if (index % 2 == 0) amplitude.toShort() else (-amplitude).toShort()
            },
            startSample = nextSample,
        )
        nextSample += RecitationAudioFormat.FRAME_SAMPLES
        return frame
    }

    private fun SpeechGate.feed(frames: List<AudioFrame>): List<AudioFrame> =
        frames.flatMap { process(it) }

    /** Past warm-up, with the noise floor settled on the room tone. */
    private fun settledGate(config: SpeechGateConfig = SpeechGateConfig()): SpeechGate =
        SpeechGate(config).apply { feed(List(30) { silence() }) }

    @Test
    fun `sustained silence is dropped once the gate has settled`() {
        val gate = settledGate()
        val before = gate.stats.framesOut

        val emitted = gate.feed(List(50) { silence() })

        assertTrue("gate leaked ${emitted.size} silent frames", emitted.isEmpty())
        assertEquals(before, gate.stats.framesOut)
        assertFalse(gate.isOpen)
    }

    @Test
    fun `speech opens the gate and is never clipped at the onset`() {
        val gate = settledGate()

        val emitted = gate.feed(List(10) { speech() })

        // Every speech frame must survive. The pre-roll adds silent frames on top, which is the
        // point — it is what guarantees the first syllable is intact.
        val speechFramesOut = emitted.count { it.rms() > 0.1f }
        assertEquals("speech frames were dropped", 10, speechFramesOut)
        assertTrue(gate.isOpen)
    }

    @Test
    fun `pre-roll replays the audio captured just before the onset`() {
        val gate = settledGate()

        val emitted = gate.feed(List(4) { speech() })

        // All four speech frames survive, and real pre-onset silence is prepended. The exact
        // total is not asserted: the pre-roll buffer already holds the first onset frame, so a
        // frame count double-counts it and only looks meaningful.
        assertEquals("speech was lost at the onset", 4, emitted.count { it.rms() > 0.1f })
        assertTrue("no pre-roll was replayed", emitted.count { it.rms() < 0.1f } > 0)
        // Pre-roll comes first, so the burst opens on silence and never mid-syllable.
        assertTrue("burst did not start with pre-roll", emitted.first().rms() < 0.1f)
    }

    @Test
    fun `a waqf keeps enough trailing silence for the server to finalize the chunk`() {
        val config = SpeechGateConfig()
        val gate = settledGate(config)
        gate.feed(List(10) { speech() })

        val tail = gate.feed(List(40) { silence() })

        // This is the assertion that protects live feedback. If the tail ever drops below the
        // server's threshold, chunks stop finalizing and the feature dies silently.
        val tailMs = tail.size * RecitationAudioFormat.FRAME_DURATION_MS
        assertTrue(
            "tail was ${tailMs}ms, below the server's ${SpeechGateConfig.MIN_TAIL_MS}ms threshold",
            tailMs >= SpeechGateConfig.MIN_TAIL_MS,
        )
        assertEquals(config.hangoverFrames, tail.size)
        assertFalse("gate stayed open through a long pause", gate.isOpen)
    }

    @Test
    fun `a short pause inside a phrase does not close the gate`() {
        val config = SpeechGateConfig()
        val gate = settledGate(config)
        gate.feed(List(5) { speech() })

        // Shorter than the hangover: a breath, not a waqf.
        val emitted = gate.feed(List(config.hangoverFrames - 1) { silence() })

        assertTrue("gate closed on a breath", gate.isOpen)
        assertEquals(config.hangoverFrames - 1, emitted.size)
    }

    @Test
    fun `an isolated click never opens the gate`() {
        val gate = settledGate()

        // One loud frame, below the onset requirement of two.
        val emitted = gate.feed(listOf(speech()) + List(10) { silence() })

        assertTrue("a click was streamed as speech", emitted.isEmpty())
        assertFalse(gate.isOpen)
    }

    @Test
    fun `a long idle stretch between phrases is what actually gets dropped`() {
        val gate = settledGate()

        gate.feed(List(10) { speech() })      // phrase
        gate.feed(List(100) { silence() })    // 10 s of dead air
        gate.feed(List(10) { speech() })      // next phrase

        // Both phrases plus two pre-rolls and one tail survive; the dead air does not.
        assertTrue(
            "gate dropped too little to be worth its risk: ${gate.stats.droppedFraction}",
            gate.stats.droppedFraction > 0.5f,
        )
    }

    @Test
    fun `speech from the very first frame is captured`() {
        // A reciter who starts the instant they tap the mic. Warm-up passes audio through
        // unconditionally precisely so this cannot be swallowed while the floor calibrates.
        val gate = SpeechGate()

        val emitted = gate.feed(List(10) { speech() })

        assertEquals(10, emitted.size)
        assertTrue(gate.isOpen)
    }

    @Test
    fun `a loud room does not permanently hold the gate open`() {
        val gate = SpeechGate()
        // Loud constant hiss, well above the initial floor estimate.
        val noisy = { frameOf(amplitude = 900) }

        gate.feed(List(60) { noisy() })
        val emitted = gate.feed(List(30) { noisy() })

        assertTrue("noise floor never adapted to a loud room", emitted.isEmpty())
        assertFalse(gate.isOpen)
    }

    @Test
    fun `sustained recitation never gates itself out`() {
        // The floor must not creep up during speech, or a long ayah eventually falls below its
        // own threshold and the reciter is cut off mid-sentence.
        val gate = settledGate()

        val emitted = gate.feed(List(200) { speech() })

        assertEquals("speech was cut during a long ayah", 200, emitted.count { it.rms() > 0.1f })
        assertTrue(gate.isOpen)
    }

    @Test
    fun `disabling the gate streams continuously`() {
        // The A/B control: matches the backend's default advice of sending everything.
        val gate = SpeechGate(SpeechGateConfig.Disabled)

        val emitted = gate.feed(List(50) { silence() })

        assertEquals(50, emitted.size)
        assertEquals(0f, gate.stats.droppedFraction, 0.0001f)
    }

    @Test
    fun `stats count every captured frame`() {
        val gate = SpeechGate()

        gate.feed(List(20) { silence() })

        assertEquals(20L, gate.stats.framesIn)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a tail shorter than the server threshold is rejected at construction`() {
        // 2 frames = 200 ms, below the server's 300 ms. Refuse rather than silently break
        // feedback for whoever tunes this later.
        SpeechGateConfig(hangoverFrames = 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a pre-roll too small to cover the onset is rejected`() {
        SpeechGateConfig(preRollFrames = 0, onsetFrames = 3)
    }
}
