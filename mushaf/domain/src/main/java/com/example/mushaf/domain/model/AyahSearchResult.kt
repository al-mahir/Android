package com.example.mushaf.domain.model

data class AyahSearchResult(
    val surahNumber: Int,
    val ayahNumber: Int,
    val ayahText: String,
    val surahNameArabic: String,
    val surahNameEnglish: String,
    val translation: String? = null,
    val score: Double? = null,
    val hydeUsed: Boolean = false
)
