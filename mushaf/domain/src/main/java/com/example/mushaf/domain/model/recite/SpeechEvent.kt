package com.example.mushaf.domain.model.recite







 
sealed interface SpeechEvent {

    class Audio(val frame: AudioFrame, val isSpeech: Boolean = true) : SpeechEvent

    data object SpeechEnded : SpeechEvent
}
