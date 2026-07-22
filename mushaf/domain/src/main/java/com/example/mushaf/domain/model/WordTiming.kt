package com.example.mushaf.domain.model

/**
 * Represents the exact timing of a single word within an audio recitation of an Ayah.
 */
data class WordTiming(
    /** 0-based index of the word within the Ayah */
    val wordIndex: Int,
    
    /** Start time in milliseconds relative to the start of the Ayah's audio */
    val startMs: Long,
    
    /** End time in milliseconds relative to the start of the Ayah's audio */
    val endMs: Long,
)
