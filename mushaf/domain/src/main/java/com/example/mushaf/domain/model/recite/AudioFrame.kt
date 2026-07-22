package com.example.mushaf.domain.model.recite

import kotlin.math.sqrt











 
class AudioFrame(
    val samples: ShortArray,
     
    val startSample: Long,
) {
    val sampleCount: Int get() = samples.size

    val startMs: Long get() = RecitationAudioFormat.durationMsOf(startSample)

    val durationMs: Long get() = RecitationAudioFormat.durationMsOf(sampleCount.toLong())

    



 
    fun rms(): Float {
        if (samples.isEmpty()) return 0f
        var sumOfSquares = 0.0
        for (sample in samples) {
            val normalised = sample / FULL_SCALE
            sumOfSquares += normalised * normalised
        }
        return sqrt(sumOfSquares / samples.size).toFloat()
    }

     
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
