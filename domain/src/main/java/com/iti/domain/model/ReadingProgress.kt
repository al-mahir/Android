package com.iti.domain.model


data class ReadingProgress(
    val surahNameAr: String,
    val surahNameEn: String,
    val ayahNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int = 1,
    val surahTotalAyahs: Int = 1,
    val surahReadAyahs: Int = 0,
)
