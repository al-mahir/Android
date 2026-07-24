package com.example.mushaf.domain.model

data class TafsirResult(
    val surahNumber: Int,
    val ayahNumber: Int,
    val tafsirText: String,
    val surahNameArabic: String = "",
    val surahNameEnglish: String = ""
)
