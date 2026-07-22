package com.example.mushaf.domain.model

/**
 * Represents the full timing metadata for an entire Ayah's recitation.
 */
data class AyahTiming(
    val surahNumber: Int,
    val ayahNumber: Int,
    
    /** Start time of the entire ayah in milliseconds */
    val timestampFrom: Long,
    
    /** End time of the entire ayah in milliseconds */
    val timestampTo: Long,

    /** Audio URL for the entire ayah */
    val audioUrl: String? = null,
    
    /** Precise timings for each word in the ayah */
    val wordTimings: List<WordTiming>,
)
