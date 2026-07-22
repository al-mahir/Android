package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.recite.SpeechEvent
import com.example.mushaf.domain.model.recite.SpeechGateConfig
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import kotlinx.coroutines.flow.Flow

/**
 * Listens to the reciter, skipping the silence between phrases.
 *
 * This is the source a live correction session streams from; [CaptureRecitationAudioUseCase] is
 * the ungated microphone, kept for capture diagnostics.
 */
class CaptureRecitationSpeechUseCase(
    private val repository: RecitationCaptureRepository,
) {
    operator fun invoke(config: SpeechGateConfig = SpeechGateConfig()): Flow<SpeechEvent> =
        repository.captureSpeech(config)
}
