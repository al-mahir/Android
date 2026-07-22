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

        
        
        val speechFramesOut = emitted.count { it.rms() > 0.1f }
        assertEquals("speech frames were dropped", 10, speechFramesOut)
        assertTrue(gate.isOpen)
    }

    @Test
    fun `pre-roll replays the audio captured just before the onset`() {
        val gate = settledGate()

        val emitted = gate.feed(List(4) { speech() })

        
        
        
        assertEquals("speech was lost at the onset", 4, emitted.count { it.rms() > 0.1f })
        assertTrue("no pre-roll was replayed", emitted.count { it.rms() < 0.1f } > 0)
        
        assertTrue("burst did not start with pre-roll", emitted.first().rms() < 0.1f)
    }

    @Test
    fun `a waqf keeps enough trailing silence for the server to finalize the chunk`() {
        val config = SpeechGateConfig()
        val gate = settledGate(config)
        gate.feed(List(10) { speech() })

        val tail = gate.feed(List(40) { silence() })

        
        
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

        
        val emitted = gate.feed(List(config.hangoverFrames - 1) { silence() })

        assertTrue("gate closed on a breath", gate.isOpen)
        assertEquals(config.hangoverFrames - 1, emitted.size)
    }

    @Test
    fun `an isolated click never opens the gate`() {
        val gate = settledGate()

        
        val emitted = gate.feed(listOf(speech()) + List(10) { silence() })

        assertTrue("a click was streamed as speech", emitted.isEmpty())
        assertFalse(gate.isOpen)
    }

    @Test
    fun `a long idle stretch between phrases is what actually gets dropped`() {
        val gate = settledGate()

        gate.feed(List(10) { speech() })      
        gate.feed(List(100) { silence() })    
        gate.feed(List(10) { speech() })      

        
        assertTrue(
            "gate dropped too little to be worth its risk: ${gate.stats.droppedFraction}",
            gate.stats.droppedFraction > 0.5f,
        )
    }

    @Test
    fun `speech from the very first frame is captured`() {
        
        
        val gate = SpeechGate()

        val emitted = gate.feed(List(10) { speech() })

        assertEquals(10, emitted.size)
        assertTrue(gate.isOpen)
    }

    @Test
    fun `a loud room does not permanently hold the gate open`() {
        val gate = SpeechGate()
        
        val noisy = { frameOf(amplitude = 900) }

        gate.feed(List(60) { noisy() })
        val emitted = gate.feed(List(30) { noisy() })

        assertTrue("noise floor never adapted to a loud room", emitted.isEmpty())
        assertFalse(gate.isOpen)
    }

    @Test
    fun `sustained recitation never gates itself out`() {
        
        
        val gate = settledGate()

        val emitted = gate.feed(List(200) { speech() })

        assertEquals("speech was cut during a long ayah", 200, emitted.count { it.rms() > 0.1f })
        assertTrue(gate.isOpen)
    }

    @Test
    fun `disabling the gate streams continuously`() {
        
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
        
        
        SpeechGateConfig(hangoverFrames = 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a pre-roll too small to cover the onset is rejected`() {
        SpeechGateConfig(preRollFrames = 0, onsetFrames = 3)
    }
}
