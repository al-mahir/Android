package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import kotlinx.coroutines.flow.Flow

class CaptureRecitationAudioUseCase(
    private val repository: RecitationCaptureRepository,
) {
    operator fun invoke(): Flow<AudioFrame> = repository.capture()
}
