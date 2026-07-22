package com.example.mushaf.domain.model.recite

/**
 * Tuning for [SpeechGate]. Frame counts are in capture frames of
 * [RecitationAudioFormat.FRAME_DURATION_MS] (100 ms each).
 *
 * The defaults are deliberately timid. Dropping audio the reciter actually produced is far worse
 * than sending a little too much: the bandwidth saving is an optimisation, but a swallowed word
 * becomes a false "missing word" mistake reported against someone who recited correctly.
 */
data class SpeechGateConfig(
    /**
     * When false every frame passes through untouched — continuous streaming, which is what
     * `docs/MOBILE_INTEGRATION.md` §6 recommends by default. Keep this switch: it is the A/B
     * control for proving the gate is not the cause of a scoring regression.
     */
    val enabled: Boolean = true,

    /** Frames replayed from before the onset, so a word never starts clipped. 300 ms. */
    val preRollFrames: Int = 3,

    /**
     * Silent frames sent *after* speech stops before the gate closes. 600 ms.
     *
     * **This is the field that must not be shortened casually.** The server finalizes a chunk
     * and pushes feedback only after its own VAD hears ≥300 ms of silence following speech. A
     * tail below that means the pause is never detected, so no feedback arrives until the 19 s
     * forced-cut cap — the gate would silently break live correction. 600 ms is 2× the
     * threshold, leaving room for frame quantisation and jitter.
     */
    val hangoverFrames: Int = 6,

    /** Consecutive speech frames needed to open the gate. 200 ms — rejects clicks and taps. */
    val onsetFrames: Int = 2,

    /**
     * Frames passed through unconditionally at the start while the noise floor calibrates.
     * Errs towards sending: a reciter who starts the instant they tap the mic must not be cut.
     */
    val warmUpFrames: Int = 5,

    /** How far above the noise floor a frame must sit to count as speech. */
    val speechFactor: Float = 2.5f,

    /**
     * Absolute RMS floor. Without it, a silent room drives the noise floor towards zero and
     * `zero * speechFactor` would classify faint hiss as speech.
     */
    val absoluteFloor: Float = 0.004f,

    /** Starting noise-floor estimate, for a moderately quiet room. */
    val initialNoiseFloor: Float = 0.02f,
) {
    init {
        require(preRollFrames >= 0) { "preRollFrames must not be negative" }
        require(onsetFrames >= 1) { "onsetFrames must be at least 1" }
        require(preRollFrames >= onsetFrames - 1) {
            "preRollFrames ($preRollFrames) must cover the onset window ($onsetFrames) or the " +
                "frames that opened the gate are dropped"
        }
        require(hangoverFrames * RecitationAudioFormat.FRAME_DURATION_MS >= MIN_TAIL_MS) {
            "hangoverFrames ($hangoverFrames) is below the server's ${MIN_TAIL_MS}ms waqf " +
                "threshold; chunks would never finalize"
        }
    }

    companion object {
        /** The server's VAD needs this much trailing silence to finalize a chunk (API.md §5.3). */
        const val MIN_TAIL_MS: Int = 300

        /** Pass-through configuration: no gating at all. */
        val Disabled: SpeechGateConfig = SpeechGateConfig(enabled = false)
    }
}

/** How much audio the gate has kept versus how much the microphone produced. */
data class SpeechGateStats(
    val framesIn: Long,
    val framesOut: Long,
) {
    /** Fraction of captured frames the gate withheld, 0f..1f. The bandwidth saving. */
    val droppedFraction: Float
        get() = if (framesIn == 0L) 0f else (framesIn - framesOut).toFloat() / framesIn
}

/**
 * Drops the stretches where nobody is reciting, so an open microphone in a quiet room does not
 * stream kilobytes of room tone to the AI service.
 *
 * **It gates, it does not strip.** Every speech burst is still surrounded by real silence —
 * [SpeechGateConfig.preRollFrames] before and [SpeechGateConfig.hangoverFrames] after — because
 * the server's own VAD reads that trailing silence as the waqf that ends a chunk. Removing it
 * would not merely save bandwidth, it would stop feedback arriving at all. What actually gets
 * dropped is the long dead air between phrases, which carries no waqf information the server has
 * not already been given.
 *
 * One consequence to keep in mind downstream: because dead air is removed, the `audio_span_sec`
 * timestamps the server reports measure *streamed* audio, not wall-clock session time. Do not
 * use them to place events on a real-time timeline.
 *
 * Not thread-safe: one instance per capture session, driven from a single collector.
 */
class SpeechGate(
    private val config: SpeechGateConfig = SpeechGateConfig(),
) {
    private val preRoll = ArrayDeque<AudioFrame>()

    private var noiseFloor = config.initialNoiseFloor
    private var consecutiveSpeech = 0
    private var consecutiveSilence = 0
    private var framesIn = 0L
    private var framesOut = 0L

    /** True while the gate is passing audio, including its trailing silence. */
    var isOpen: Boolean = false
        private set

    val stats: SpeechGateStats get() = SpeechGateStats(framesIn, framesOut)

    /** Current noise-floor estimate. Exposed for diagnostics, not for control flow. */
    val noiseFloorEstimate: Float get() = noiseFloor

    /**
     * Feeds one captured frame in and returns the frames that should go on the wire — none while
     * the gate is shut, and a burst of buffered pre-roll on the frame that opens it.
     *
     * Check [isOpen] before and after to detect the close, which marks a waqf boundary.
     */
    fun process(frame: AudioFrame): List<AudioFrame> {
        framesIn++

        if (!config.enabled) {
            framesOut++
            isOpen = true
            return listOf(frame)
        }

        val isWarmingUp = framesIn <= config.warmUpFrames
        val rms = frame.rms()
        // Classify against the floor as it stood before this frame, then let a non-speech frame
        // refine it. Speech never raises the floor, or a long ayah would slowly gate itself out.
        val isSpeech = isWarmingUp || rms > maxOf(noiseFloor * config.speechFactor, config.absoluteFloor)
        updateNoiseFloor(rms, isSpeech && !isWarmingUp)

        if (isSpeech) {
            consecutiveSpeech++
            consecutiveSilence = 0
        } else {
            consecutiveSilence++
            consecutiveSpeech = 0
        }

        return if (isOpen) passWhileOpen(frame) else considerOpening(frame)
    }

    /** While open every frame is sent, so the silence that closes the gate becomes the tail. */
    private fun passWhileOpen(frame: AudioFrame): List<AudioFrame> {
        if (consecutiveSilence >= config.hangoverFrames) {
            isOpen = false
        }
        framesOut++
        return listOf(frame)
    }

    private fun considerOpening(frame: AudioFrame): List<AudioFrame> {
        if (consecutiveSpeech < config.onsetFrames) {
            rememberForPreRoll(frame)
            return emptyList()
        }

        isOpen = true
        // The pre-roll holds the frames just before this one — including the earlier frames of
        // the onset run, which is why config requires it to cover the onset window.
        val burst = ArrayList<AudioFrame>(preRoll.size + 1).apply {
            addAll(preRoll)
            add(frame)
        }
        preRoll.clear()
        framesOut += burst.size
        return burst
    }

    private fun rememberForPreRoll(frame: AudioFrame) {
        if (config.preRollFrames == 0) return
        preRoll.addLast(frame)
        while (preRoll.size > config.preRollFrames) {
            preRoll.removeFirst()
        }
    }

    private fun updateNoiseFloor(rms: Float, isSpeech: Boolean) {
        noiseFloor = when {
            // Track downwards quickly: a quieter room should be believed immediately.
            rms < noiseFloor -> noiseFloor + (rms - noiseFloor) * FLOOR_FALL_RATE
            // Never let speech pull the floor up, or sustained recitation raises the bar until
            // it gates itself out mid-ayah.
            isSpeech -> noiseFloor
            // Rising room noise is believed only slowly.
            else -> noiseFloor + (rms - noiseFloor) * FLOOR_RISE_RATE
        }
    }

    private companion object {
        const val FLOOR_FALL_RATE = 0.5f
        const val FLOOR_RISE_RATE = 0.01f
    }
}
