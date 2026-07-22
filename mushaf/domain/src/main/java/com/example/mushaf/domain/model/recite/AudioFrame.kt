package com.example.mushaf.domain.model.recite

import kotlin.math.sqrt

/**
 * One captured block of the reciter's voice: signed 16-bit PCM mono samples at
 * [RecitationAudioFormat.SAMPLE_RATE_HZ].
 *
 * [samples] is handed over by the producer and MUST NOT be mutated afterwards — every stage
 * downstream (speech gate, debug sink, socket) reads it concurrently. Producers emit a fresh
 * array per frame rather than reusing one buffer.
 *
 * Deliberately not a `data class`: array `equals`/`hashCode` are identity-based and would make
 * the generated implementations quietly wrong.
 */
class AudioFrame(
    val samples: ShortArray,
    /** Offset of this frame's first sample from the start of the capture. */
    val startSample: Long,
) {
    val sampleCount: Int get() = samples.size

    val startMs: Long get() = RecitationAudioFormat.durationMsOf(startSample)

    val durationMs: Long get() = RecitationAudioFormat.durationMsOf(sampleCount.toLong())

    /**
     * Root-mean-square amplitude normalised to 0f..1f, the loudness measure the speech gate and
     * the on-screen level meter both read. Uses 32768 (not [Short.MAX_VALUE]) as full scale so a
     * full-negative-swing frame cannot exceed 1f.
     */
    fun rms(): Float {
        if (samples.isEmpty()) return 0f
        var sumOfSquares = 0.0
        for (sample in samples) {
            val normalised = sample / FULL_SCALE
            sumOfSquares += normalised * normalised
        }
        return sqrt(sumOfSquares / samples.size).toFloat()
    }

    /** Loudest absolute sample in the frame, normalised to 0f..1f. Detects clipping. */
    fun peak(): Float {
        var loudest = 0
        for (sample in samples) {
            val magnitude = if (sample < 0) -sample.toInt() else sample.toInt()
            if (magnitude > loudest) loudest = magnitude
        }
        return (loudest / FULL_SCALE).toFloat()
    }

    private companion object {
        const val FULL_SCALE = 32768.0
    }
}
