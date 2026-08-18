package com.example.mushaf.domain.model.recite







 
object RecitationAudioFormat {

     
    const val SAMPLE_RATE_HZ: Int = 16_000

     
    const val CHANNEL_COUNT: Int = 1

     
    const val BITS_PER_SAMPLE: Int = 16

    const val BYTES_PER_SAMPLE: Int = BITS_PER_SAMPLE / Byte.SIZE_BITS

    /**
     * How much audio one captured frame carries.
     *
     * 32ms, not the 100ms the web client uses, and the difference is not cosmetic.
     * `AudioRecord.read()` blocks until a whole frame is filled, so the last sample a reciter
     * speaks before a waqf sits in our buffer for up to a full frame before it is even eligible to
     * be sent - and the server cannot advance its 1536-sample VAD window over audio it has not
     * received. At 100ms that was 0-100ms of dead time in front of every endpoint decision; at
     * 32ms it is 0-32ms. The web's AudioWorklet has no blocking read behind its 100ms, so its
     * frame size is a cadence rather than a stall; ours is a stall, so it has to be smaller.
     *
     * 512 samples exactly, which is a third of the server's 1536-sample VAD window - three frames
     * fill one window with no residue - and a power of two for `AudioRecord`.
     */
    const val FRAME_DURATION_MS: Int = 32


    const val FRAME_SAMPLES: Int = SAMPLE_RATE_HZ * FRAME_DURATION_MS / 1000


    const val FRAME_BYTES: Int = FRAME_SAMPLES * BYTES_PER_SAMPLE


    fun durationMsOf(sampleCount: Long): Long =
        sampleCount * 1000L / SAMPLE_RATE_HZ

    /**
     * Frames needed to cover [durationMs], rounded up.
     *
     * Anything tuned in milliseconds - a hangover tail, a pre-roll, a warm-up - must be expressed
     * through this rather than as a frame count, or it silently changes meaning the next time
     * [FRAME_DURATION_MS] moves.
     */
    fun framesFor(durationMs: Int): Int {
        require(durationMs >= 0) { "durationMs=$durationMs must not be negative" }
        return (durationMs + FRAME_DURATION_MS - 1) / FRAME_DURATION_MS
    }
}
