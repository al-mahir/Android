package com.example.mushaf.domain.model



 
data class AyahTiming(
    val surahNumber: Int,
    val ayahNumber: Int,
    
     
    val timestampFrom: Long,
    
     
    val timestampTo: Long,

    /** Audio URL for the entire ayah */
    val audioUrl: String? = null,
    
     
    val wordTimings: List<WordTiming>,
)
