package com.example.mushaf.domain

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import com.example.mushaf.domain.model.recite.SpeechGate
import com.example.mushaf.domain.model.recite.SpeechGateConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test










 
class SpeechGateTest {

    private var nextSample = 0L

     
    private fun silence() = frameOf(amplitude = 30)

     
    private fun speech() = frameOf(amplitude = 8_000)

    private fun frameOf(amplitude: Int): AudioFrame {
        val frame = AudioFrame(
            samples = ShortArray(RecitationAudioFormat.FRAME_SAMPLES) { index ->
                
                if (index % 2 == 0) amplitude.toShort() else (-amplitude).toShort()
            },
            startSample = nextSample,
        )
        nextSample += RecitationAudioFormat.FRAME_SAMPLES
        return frame
    }

    private fun SpeechGate.feed(frames: List<AudioFrame>): List<AudioFrame> =
        frames.flatMap { process(it) }

    /**
     * Everything here is written in milliseconds rather than frame counts.
     *
     * A frame is 32ms now and was 100ms before; a test that says "feed 4 speech frames" means
     * something different at each size, and the gate's own thresholds (onset, hangover, pre-roll,
     * warm-up) are all durations. Frame counts here would pass or fail on the frame size rather
     * than on the behaviour being tested.
     */
    private fun framesOf(durationMs: Int, frame: () -> AudioFrame): List<AudioFrame> =
        List(RecitationAudioFormat.framesFor(durationMs)) { frame() }

    /** Long enough for the noise floor to settle and the warm-up window to close behind it. */
    private fun settledGate(config: SpeechGateConfig = SpeechGateConfig()): SpeechGate =
        SpeechGate(config).apply { feed(framesOf(3_000) { silence() }) }

    @Test
    fun `sustained silence is dropped once the gate has settled`() {
        val gate = settledGate()
        val before = gate.stats.framesOut

        val emitted = gate.feed(framesOf(5_000) { silence() })

        assertTrue("gate leaked ${emitted.size} silent frames", emitted.isEmpty())
        assertEquals(before, gate.stats.framesOut)
        assertFalse(gate.isOpen)
    }

    @Test
    fun `speech opens the gate and is never clipped at the onset`() {
        val gate = settledGate()

        val spoken = framesOf(1_000) { speech() }
        val emitted = gate.feed(spoken)

        
        
        val speechFramesOut = emitted.count { it.rms() > 0.1f }
        assertEquals("speech frames were dropped", spoken.size, speechFramesOut)
        assertTrue(gate.isOpen)
    }

    @Test
    fun `pre-roll replays the audio captured just before the onset`() {
        val gate = settledGate()

        val spoken = framesOf(400) { speech() }
        val emitted = gate.feed(spoken)

        
        
        
        assertEquals("speech was lost at the onset", spoken.size, emitted.count { it.rms() > 0.1f })
        assertTrue("no pre-roll was replayed", emitted.count { it.rms() < 0.1f } > 0)
        
        assertTrue("burst did not start with pre-roll", emitted.first().rms() < 0.1f)
    }

    @Test
    fun `a waqf keeps enough trailing silence for the server to finalize the chunk`() {
        val config = SpeechGateConfig()
        val gate = settledGate(config)
        gate.feed(framesOf(1_000) { speech() })

        val tail = gate.feed(framesOf(4_000) { silence() })

        
        
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
        gate.feed(framesOf(1_000) { speech() })

        
        val emitted = gate.feed(List(config.hangoverFrames - 1) { silence() })

        assertTrue("gate closed on a breath", gate.isOpen)
        assertEquals(config.hangoverFrames - 1, emitted.size)
    }

    @Test
    fun `an isolated click never opens the gate`() {
        val gate = settledGate()

        
        val emitted = gate.feed(listOf(speech()) + framesOf(1_000) { silence() })

        assertTrue("a click was streamed as speech", emitted.isEmpty())
        assertFalse(gate.isOpen)
    }

    @Test
    fun `a long idle stretch between phrases is what actually gets dropped`() {
        val gate = settledGate()

        gate.feed(framesOf(1_000) { speech() })      
        gate.feed(framesOf(10_000) { silence() })    
        gate.feed(framesOf(1_000) { speech() })      

        
        assertTrue(
            "gate dropped too little to be worth its risk: ${gate.stats.droppedFraction}",
            gate.stats.droppedFraction > 0.5f,
        )
    }

    @Test
    fun `speech from the very first frame is captured`() {
        
        
        val gate = SpeechGate()

        val spoken = framesOf(1_000) { speech() }
        val emitted = gate.feed(spoken)

        assertEquals(spoken.size, emitted.size)
        assertTrue(gate.isOpen)
    }

    @Test
    fun `a loud room does not permanently hold the gate open`() {
        val gate = SpeechGate()
        
        val noisy = { frameOf(amplitude = 900) }

        gate.feed(framesOf(6_000) { noisy() })
        val emitted = gate.feed(framesOf(3_000) { noisy() })

        assertTrue("noise floor never adapted to a loud room", emitted.isEmpty())
        assertFalse(gate.isOpen)
    }

    @Test
    fun `sustained recitation never gates itself out`() {
        
        
        val gate = settledGate()

        val spoken = framesOf(20_000) { speech() }
        val emitted = gate.feed(spoken)

        assertEquals("speech was cut during a long ayah", spoken.size, emitted.count { it.rms() > 0.1f })
        assertTrue(gate.isOpen)
    }

    @Test
    fun `disabling the gate streams continuously`() {
        
        val gate = SpeechGate(SpeechGateConfig.Disabled)

        val silent = framesOf(5_000) { silence() }
        val emitted = gate.feed(silent)

        assertEquals(silent.size, emitted.size)
        assertEquals(0f, gate.stats.droppedFraction, 0.0001f)
    }

    @Test
    fun `stats count every captured frame`() {
        val gate = SpeechGate()

        val silent = framesOf(2_000) { silence() }
        gate.feed(silent)

        assertEquals(silent.size.toLong(), gate.stats.framesIn)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a tail shorter than the server threshold is rejected at construction`() {
        
        
        SpeechGateConfig(hangoverFrames = 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a pre-roll too small to cover the onset is rejected`() {
        SpeechGateConfig(preRollFrames = 0, onsetFrames = 3)
    }

    @Test
    fun `warm-up frames are forwarded but never called speech`() {
        val gate = SpeechGate()

        
        
        val passed = gate.feed(List(SpeechGateConfig().warmUpFrames) { silence() })

        assertTrue("warm-up frames were dropped", passed.isNotEmpty())
        assertFalse(
            "opening silence was reported as speech, which walks the cursor forward before " +
                "the reciter has said anything",
            gate.isSpeechFrame,
        )
    }

    @Test
    fun `the waqf tail is forwarded but never called speech`() {
        val gate = settledGate()
        gate.feed(framesOf(400) { speech() })
        assertTrue("speech was not recognised", gate.isSpeechFrame)

        
        
        val tail = gate.feed(framesOf(64) { silence() })

        assertTrue("the waqf tail was dropped", tail.isNotEmpty())
        assertTrue("the gate closed before the tail was sent", gate.isOpen)
        assertFalse("the waqf tail was reported as speech", gate.isSpeechFrame)
    }

    @Test
    fun `real speech during warm-up is still recognised`() {
        val gate = SpeechGate()

        gate.feed(framesOf(64) { speech() })

        
        assertTrue(gate.isSpeechFrame)
    }

    @Test
    fun `a disabled gate still detects speech`() {
        // The gate is off on the wire path so the server's Silero VAD sees the reciter's real
        // timeline - but the mic meter, the isSpeaking flag and SpeechEnded all read this gate,
        // so detection has to keep running even when nothing is being withheld.
        val gate = SpeechGate(SpeechGateConfig.Disabled)

        gate.feed(framesOf(3_000) { silence() })
        assertFalse("silence was reported as speech", gate.isSpeechFrame)

        gate.feed(framesOf(400) { speech() })
        assertTrue("speech went unnoticed once the gate stopped withholding", gate.isSpeechFrame)
        assertTrue("the open/close state machine stopped running", gate.isOpen)
    }

    @Test
    fun `a disabled gate withholds nothing, not even between phrases`() {
        val gate = SpeechGate(SpeechGateConfig.Disabled)

        val all = framesOf(1_000) { speech() } + framesOf(5_000) { silence() } + framesOf(1_000) { speech() }
        val emitted = gate.feed(all)

        // The whole point of §3.3: no pre-roll burst after a gap, no silence spliced out from
        // under the server's sample counter. What the microphone heard is what the socket sends.
        assertEquals(all.size, emitted.size)
        assertEquals(0f, gate.stats.droppedFraction, 0.0001f)
    }
}
