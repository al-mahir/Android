package com.iti.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReadingProgressDto(
    @SerialName("surah_name") val surahName: String,
    @SerialName("ayah_number") val ayahNumber: Int,
    @SerialName("page_number") val pageNumber: Int,
)
