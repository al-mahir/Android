package com.example.mushaf.domain

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AudioFrameTest {

    @Test
    fun `silence has zero loudness`() {
        val frame = AudioFrame(ShortArray(RecitationAudioFormat.FRAME_SAMPLES), startSample = 0)

        assertEquals(0f, frame.rms(), TOLERANCE)
        assertEquals(0f, frame.peak(), TOLERANCE)
    }

    @Test
    fun `full-scale square wave reads as full loudness`() {
        val samples = ShortArray(100) { if (it % 2 == 0) Short.MAX_VALUE else Short.MIN_VALUE }

        val frame = AudioFrame(samples, startSample = 0)

        assertEquals(1f, frame.rms(), 0.001f)
        assertEquals(1f, frame.peak(), TOLERANCE)
    }

    @Test
    fun `sine wave rms is the expected fraction of its peak`() {
        
        val samples = ShortArray(RecitationAudioFormat.SAMPLE_RATE_HZ / 100) { index ->
            (16384 * sin(2 * PI * index / 160.0)).toInt().toShort()
        }

        val frame = AudioFrame(samples, startSample = 0)

        assertEquals(0.354f, frame.rms(), 0.01f)
        assertEquals(0.5f, frame.peak(), 0.01f)
    }

    @Test
    fun `most-negative sample cannot push loudness above full scale`() {
        
        
        val frame = AudioFrame(ShortArray(16) { Short.MIN_VALUE }, startSample = 0)

        assertTrue("peak=${frame.peak()} exceeded full scale", frame.peak() <= 1f)
        assertTrue("rms=${frame.rms()} exceeded full scale", frame.rms() <= 1f)
    }

    @Test
    fun `frame timing follows the 16 kHz sample rate`() {
        val frame = AudioFrame(
            samples = ShortArray(RecitationAudioFormat.FRAME_SAMPLES),
            startSample = RecitationAudioFormat.SAMPLE_RATE_HZ.toLong(), 
        )

        assertEquals(1_000L, frame.startMs)
        assertEquals(RecitationAudioFormat.FRAME_DURATION_MS.toLong(), frame.durationMs)
    }

    @Test
    fun `wire format matches the service contract`() {
        
        assertEquals(16_000, RecitationAudioFormat.SAMPLE_RATE_HZ)
        assertEquals(1, RecitationAudioFormat.CHANNEL_COUNT)
        assertEquals(16, RecitationAudioFormat.BITS_PER_SAMPLE)
        // 512 samples, a third of the server's 1536-sample VAD window, so three frames fill one
        // window exactly and the endpoint decision is never waiting on a partial one.
        assertEquals(512, RecitationAudioFormat.FRAME_SAMPLES)
        assertEquals(1_024, RecitationAudioFormat.FRAME_BYTES)
    }

    @Test
    fun `frames for a duration always cover it`() {
        assertEquals(0, RecitationAudioFormat.framesFor(0))
        assertEquals(1, RecitationAudioFormat.framesFor(1))
        assertEquals(1, RecitationAudioFormat.framesFor(RecitationAudioFormat.FRAME_DURATION_MS))
        assertEquals(2, RecitationAudioFormat.framesFor(RecitationAudioFormat.FRAME_DURATION_MS + 1))

        // The property the callers actually depend on: a tail sized through this is never short of
        // what was asked for, which is what keeps SpeechGateConfig's waqf-threshold check honest.
        for (durationMs in listOf(1, 99, 300, 500, 600, 800, 8_000)) {
            val covered = RecitationAudioFormat.framesFor(durationMs) * RecitationAudioFormat.FRAME_DURATION_MS
            assertTrue("framesFor($durationMs) covers only ${covered}ms", covered >= durationMs)
        }
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
