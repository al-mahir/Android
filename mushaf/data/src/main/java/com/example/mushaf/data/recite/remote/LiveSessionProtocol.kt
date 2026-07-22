package com.example.mushaf.data.recite.remote

import com.example.mushaf.data.recite.remote.dto.FeedbackEnvelopeDto
import com.example.mushaf.domain.model.recite.AudioFrame

 
sealed interface LiveSessionCommand {

     
    class Audio(val frame: AudioFrame) : LiveSessionCommand

    


 
    data class Seek(val sura: Int, val aya: Int, val wordIdx: Int = 0) : LiveSessionCommand

    



 
    data object End : LiveSessionCommand
}

 
sealed interface LiveSessionEvent {

    





 
    data class Started(
        val sessionId: String,
        val engine: String,
        val sampleRate: Int,
        val requestedEngine: String?,
    ) : LiveSessionEvent {
         
        val engineSubstituted: Boolean
            get() = requestedEngine != null && !requestedEngine.equals(engine, ignoreCase = true)
    }

     
    data class Feedback(val envelope: FeedbackEnvelopeDto) : LiveSessionEvent

     
    data object Done : LiveSessionEvent
}






 
class LiveSessionException(
    message: String,
    val closeCode: Short? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
