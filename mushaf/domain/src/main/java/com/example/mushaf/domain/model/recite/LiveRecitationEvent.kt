package com.example.mushaf.domain.model.recite

import com.example.mushaf.domain.model.recite.local.LocalTranscript


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

    /**
     * The on-device model's running phoneme transcript, forwarded roughly every 100ms. Advisory
     * only: it moves the predicted highlight and nothing else — grading, mistake marks and page
     * turns all stay driven by [Graded].
     */
    data class LocalPhonemes(val transcript: LocalTranscript) : LiveRecitationEvent


    data object Finished : LiveRecitationEvent
}

 
sealed interface RecitationControl {

    




 
    data class Seek(val position: RecitationCursor) : RecitationControl

    




 
    data object Finish : RecitationControl
}
