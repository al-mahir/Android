package com.iti.domain.model


data class ReadingProgress(
    /** 1-based surah number — the identity the UI resolves a localized name from. */
    val surahNumber: Int,
    /** English transliteration; a fallback for UI that cannot resolve a localized name. */
    val surahName: String,
    val ayahNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int = 1,
    val surahTotalAyahs: Int = 1,
    val surahReadAyahs: Int = 0,
)
