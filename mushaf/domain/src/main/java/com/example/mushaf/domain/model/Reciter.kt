package com.example.mushaf.domain.model

/**
 * Represents a Quran reciter available in the catalog.
 */
data class Reciter(
    val id: Int,
    val name: String,
    val nameArabic: String,
    val style: RecitationStyle,
    val audioBaseUrl: String,
)
