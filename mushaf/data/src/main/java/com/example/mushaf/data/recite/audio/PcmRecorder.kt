package com.example.mushaf.data.recite.audio

import com.example.mushaf.domain.model.recite.AudioFrame
import kotlinx.coroutines.flow.Flow






 
interface PcmRecorder {

    


 
    fun record(): Flow<AudioFrame>
}
