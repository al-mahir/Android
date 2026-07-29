package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.SpeechEvent
import com.example.mushaf.domain.model.recite.SpeechGateConfig
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow

interface RecitationCaptureRepository {

    fun capture(): Flow<Result<AudioFrame>>

    fun captureSpeech(config: SpeechGateConfig = SpeechGateConfig()): Flow<Result<SpeechEvent>>
}
