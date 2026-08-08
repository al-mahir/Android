package com.example.mushaf.domain.model.recite

 
sealed interface LiveRecitationEvent {

    





 
    data class Started(
        val sessionId: String,
        val engine: String,
        val requestedEngine: String?,
    ) : LiveRecitationEvent {
        val engineSubstituted: Boolean
            get() = requestedEngine != null && !requestedEngine.equals(engine, ignoreCase = true)
    }

    




 
    data class Level(val amplitude: Float, val isSpeaking: Boolean) : LiveRecitationEvent


    data class Graded(val chunk: RecitationChunk) : LiveRecitationEvent

    data class LocalWord(val word: String) : LiveRecitationEvent


    data object Finished : LiveRecitationEvent
}

 
sealed interface RecitationControl {

    




 
    data class Seek(val position: RecitationCursor) : RecitationControl

    




 
    data object Finish : RecitationControl
}
