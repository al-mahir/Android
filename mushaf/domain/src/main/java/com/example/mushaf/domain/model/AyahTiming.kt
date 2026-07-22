package com.example.mushaf.domain.model



 
data class AyahTiming(
    val surahNumber: Int,
    val ayahNumber: Int,
    
     
    val timestampFrom: Long,
    
     
    val timestampTo: Long,
    
     
    val wordTimings: List<WordTiming>,
)
