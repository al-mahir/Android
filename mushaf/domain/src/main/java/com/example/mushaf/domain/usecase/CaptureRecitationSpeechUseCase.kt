package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.recite.SpeechEvent
import com.example.mushaf.domain.model.recite.SpeechGateConfig
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow






 
class CaptureRecitationSpeechUseCase(
    private val repository: RecitationCaptureRepository,
) {
    operator fun invoke(config: SpeechGateConfig = SpeechGateConfig()): Flow<Result<SpeechEvent>> =
        repository.captureSpeech(config)
}
