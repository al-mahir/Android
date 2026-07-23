package com.example.mushaf.domain.model.recite







 
object RecitationAudioFormat {

     
    const val SAMPLE_RATE_HZ: Int = 16_000

     
    const val CHANNEL_COUNT: Int = 1

     
    const val BITS_PER_SAMPLE: Int = 16

    const val BYTES_PER_SAMPLE: Int = BITS_PER_SAMPLE / Byte.SIZE_BITS

     
    const val FRAME_DURATION_MS: Int = 100

     
    const val FRAME_SAMPLES: Int = SAMPLE_RATE_HZ * FRAME_DURATION_MS / 1000

     
    const val FRAME_BYTES: Int = FRAME_SAMPLES * BYTES_PER_SAMPLE

     
    fun durationMsOf(sampleCount: Long): Long =
        sampleCount * 1000L / SAMPLE_RATE_HZ
}
