package com.example.mushaf.domain.model.recite

/**
 * The wire format the Al-Mahir AI service expects on `WS /ws/session` (see `docs/API.md` §5.3).
 *
 * These are not tunable preferences — the service accepts exactly one format, so every stage of
 * the capture pipeline (recorder, speech gate, socket) reads its constants from here rather than
 * repeating literals that could drift apart.
 */
object RecitationAudioFormat {

    /** The service's `sample_rate` is always 16000; it resamples nothing. */
    const val SAMPLE_RATE_HZ: Int = 16_000

    /** Mono. Stereo frames are one of the mistakes the API doc calls out by name. */
    const val CHANNEL_COUNT: Int = 1

    /** Signed 16-bit PCM, little-endian, no container and no WAV header. */
    const val BITS_PER_SAMPLE: Int = 16

    const val BYTES_PER_SAMPLE: Int = BITS_PER_SAMPLE / Byte.SIZE_BITS

    /** API.md: "100 ms (3200 bytes) works well". */
    const val FRAME_DURATION_MS: Int = 100

    /** Samples in one capture frame: 1600. */
    const val FRAME_SAMPLES: Int = SAMPLE_RATE_HZ * FRAME_DURATION_MS / 1000

    /** Bytes on the wire for one capture frame: 3200. */
    const val FRAME_BYTES: Int = FRAME_SAMPLES * BYTES_PER_SAMPLE

    /** Milliseconds of audio in [sampleCount] samples at [SAMPLE_RATE_HZ]. */
    fun durationMsOf(sampleCount: Long): Long =
        sampleCount * 1000L / SAMPLE_RATE_HZ
}
