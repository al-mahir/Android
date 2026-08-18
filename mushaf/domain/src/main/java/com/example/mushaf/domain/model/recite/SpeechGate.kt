package com.example.mushaf.domain.model.recite

data class SpeechGateConfig(

    /**
     * Whether silence is actually withheld from the caller.
     *
     * Off on the live-correction wire path (see `LiveRecitationConfig.speechGate`). Detection runs
     * either way - [SpeechGate.isSpeechFrame] and [SpeechGate.isOpen] mean the same thing at both
     * settings - so the mic meter keeps working on a gate that streams continuously.
     */
    val enabled: Boolean = true,

    val preRollFrames: Int = RecitationAudioFormat.framesFor(300),

    val hangoverFrames: Int = RecitationAudioFormat.framesFor(600),

    val onsetFrames: Int = RecitationAudioFormat.framesFor(200),

    val warmUpFrames: Int = RecitationAudioFormat.framesFor(500),

    val speechFactor: Float = 2.5f,

    val absoluteFloor: Float = 0.004f,
     
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

        const val MIN_TAIL_MS: Int = 300

        /**
         * Streams every captured frame, while still reporting speech/silence.
         *
         * The default for the live-correction socket: the server's Silero VAD wants the reciter's
         * real timeline, not one we have edited. Detection stays on because the mic meter, the
         * `isSpeaking` flag and `SpeechEvent.SpeechEnded` all read it.
         */
        val Disabled: SpeechGateConfig = SpeechGateConfig(enabled = false)
    }
}

 
data class SpeechGateStats(
    val framesIn: Long,
    val framesOut: Long,
) {
     
    val droppedFraction: Float
        get() = if (framesIn == 0L) 0f else (framesIn - framesOut).toFloat() / framesIn
}


class SpeechGate(
    private val config: SpeechGateConfig = SpeechGateConfig(),
) {
    private val preRoll = ArrayDeque<AudioFrame>()

    private var noiseFloor = config.initialNoiseFloor
    private var consecutiveSpeech = 0
    private var consecutiveSilence = 0
    private var framesIn = 0L
    private var framesOut = 0L


    var isOpen: Boolean = false
        private set

    var isSpeechFrame: Boolean = false
        private set

    val stats: SpeechGateStats get() = SpeechGateStats(framesIn, framesOut)

     
    val noiseFloorEstimate: Float get() = noiseFloor

    fun process(frame: AudioFrame): List<AudioFrame> {
        framesIn++

        val isWarmingUp = framesIn <= config.warmUpFrames
        val rms = frame.rms()

        val exceedsThreshold =
            rms > maxOf(noiseFloor * config.speechFactor, config.absoluteFloor)

        val isSpeech = isWarmingUp || exceedsThreshold
        isSpeechFrame = exceedsThreshold

        updateNoiseFloor(rms, isSpeech && !isWarmingUp)

        if (isSpeech) {
            consecutiveSpeech++
            consecutiveSilence = 0
        } else {
            consecutiveSilence++
            consecutiveSpeech = 0
        }

        // The state machine runs at both settings, so `isOpen`/`isSpeechFrame` mean the same thing
        // whether or not anything is being withheld. Only what comes back out differs.
        val gated = if (isOpen) passWhileOpen(frame) else considerOpening(frame)

        // Streaming continuously is what the live-correction path wants: the server runs its own
        // Silero VAD, and a burst-after-gap - which is exactly what the pre-roll flush looks like
        // on the wire - is worse for a stateful RNN endpointer than the silence it saves sending.
        // The gated result is computed and discarded rather than skipped: the pre-roll buffer and
        // the open/close transitions have to stay live for the meter to read them.
        val emitted = if (config.enabled) gated else listOf(frame)
        framesOut += emitted.size
        return emitted
    }


    private fun passWhileOpen(frame: AudioFrame): List<AudioFrame> {
        if (consecutiveSilence >= config.hangoverFrames) {
            isOpen = false
        }
        return listOf(frame)
    }

    private fun considerOpening(frame: AudioFrame): List<AudioFrame> {
        if (consecutiveSpeech < config.onsetFrames) {
            rememberForPreRoll(frame)
            return emptyList()
        }

        isOpen = true


        val burst = ArrayList<AudioFrame>(preRoll.size + 1).apply {
            addAll(preRoll)
            add(frame)
        }
        preRoll.clear()
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
            
            rms < noiseFloor -> noiseFloor + (rms - noiseFloor) * FLOOR_FALL_RATE
            
            
            isSpeech -> noiseFloor
            
            else -> noiseFloor + (rms - noiseFloor) * FLOOR_RISE_RATE
        }
    }

    private companion object {
        const val FLOOR_FALL_RATE = 0.5f
        const val FLOOR_RISE_RATE = 0.01f
    }
}
