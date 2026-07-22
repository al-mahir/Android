package com.example.mushaf.data.recite.audio

import com.example.mushaf.domain.model.recite.AudioFrame
import kotlinx.coroutines.flow.Flow

/**
 * Microphone data source, declared as an interface so a stand-in can be swapped for the real
 * one without touching any layer above (AGENTS.md → Data flow shape). Step 3 of the Ta'ahud
 * plan adds a WAV-replay implementation so the whole pipeline can be exercised without
 * speaking into a device.
 */
interface PcmRecorder {

    /**
     * Cold flow of 16 kHz mono PCM16 frames. Recording starts on collection and the underlying
     * device is released when collection ends, however it ends.
     */
    fun record(): Flow<AudioFrame>
}
