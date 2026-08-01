package com.iti.domain.model


/**
 * A single Ayah randomly selected for display each day on the Home screen.
 *
 * Business-domain concept: the screen is one consumer; any other feature (notifications,
 * widgets) can reuse the same model without a rename.
 */
data class AyahOfTheDay(
    /** Arabic text of the ayah (with full tashkeel). */
    val arabicText: String,
    /** Name of the surah this ayah belongs to (Arabic). */
    val surahName: String,
    /** 1-based ayah number within the surah. */
    val ayahNumber: Int,
    /** 1-based surah number. */
    val surahNumber: Int,
)
