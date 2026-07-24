package com.iti.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReadingProgressDto(
    @SerialName("surah_name") val surahName: String,
    @SerialName("ayah_number") val ayahNumber: Int,
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("juz_number") val juzNumber: Int = 1,
    @SerialName("surah_total_ayahs") val surahTotalAyahs: Int = 1,
    @SerialName("surah_read_ayahs") val surahReadAyahs: Int = 0,
)

