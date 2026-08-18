package com.example.mushaf.domain.model

/**
 * A user's personal note attached to a specific ayah in the Mushaf.
 */
data class AyahNote(
    val surahNumber: Int,
    val ayahNumber: Int,
    val text: String,
    val updatedAtEpochMs: Long,
)
