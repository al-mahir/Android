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
        // A sine's RMS is peak / sqrt(2) ~= 0.707 * peak. Half-scale peak -> ~0.354 RMS.
        val samples = ShortArray(RecitationAudioFormat.SAMPLE_RATE_HZ / 100) { index ->
            (16384 * sin(2 * PI * index / 160.0)).toInt().toShort()
        }

        val frame = AudioFrame(samples, startSample = 0)

        assertEquals(0.354f, frame.rms(), 0.01f)
        assertEquals(0.5f, frame.peak(), 0.01f)
    }

    @Test
    fun `most-negative sample cannot push loudness above full scale`() {
        // Short.MIN_VALUE is -32768: negating it overflows back to itself, the classic way a
        // level meter reports a nonsensical value just above 1.
        val frame = AudioFrame(ShortArray(16) { Short.MIN_VALUE }, startSample = 0)

        assertTrue("peak=${frame.peak()} exceeded full scale", frame.peak() <= 1f)
        assertTrue("rms=${frame.rms()} exceeded full scale", frame.rms() <= 1f)
    }

    @Test
    fun `frame timing follows the 16 kHz sample rate`() {
        val frame = AudioFrame(
            samples = ShortArray(RecitationAudioFormat.FRAME_SAMPLES),
            startSample = RecitationAudioFormat.SAMPLE_RATE_HZ.toLong(), // Exactly one second in.
        )

        assertEquals(1_000L, frame.startMs)
        assertEquals(RecitationAudioFormat.FRAME_DURATION_MS.toLong(), frame.durationMs)
    }

    @Test
    fun `wire format matches the service contract`() {
        // These are the values docs/API.md 5_3 fixes; drift here is silently decoded as noise.
        assertEquals(16_000, RecitationAudioFormat.SAMPLE_RATE_HZ)
        assertEquals(1, RecitationAudioFormat.CHANNEL_COUNT)
        assertEquals(16, RecitationAudioFormat.BITS_PER_SAMPLE)
        assertEquals(1_600, RecitationAudioFormat.FRAME_SAMPLES)
        assertEquals(3_200, RecitationAudioFormat.FRAME_BYTES)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
