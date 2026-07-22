package com.example.mushaf.domain.model.recite







 
sealed interface SpeechEvent {

     
    class Audio(val frame: AudioFrame) : SpeechEvent

    






 
    data object SpeechEnded : SpeechEvent
}
