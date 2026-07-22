package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.SpeechEvent
import com.example.mushaf.domain.model.recite.SpeechGateConfig
import kotlinx.coroutines.flow.Flow







 
interface RecitationCaptureRepository {

    






 
    fun capture(): Flow<AudioFrame>

    








 
    fun captureSpeech(config: SpeechGateConfig = SpeechGateConfig()): Flow<SpeechEvent>
}
