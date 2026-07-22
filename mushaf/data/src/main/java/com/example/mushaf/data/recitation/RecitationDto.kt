package com.example.mushaf.data.recitation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReciterDto(
    val id: Int,
    val name: String,
    @SerialName("name_arabic") val nameArabic: String,
    val style: String, // e.g. "Murattal"
    @SerialName("audio_base_url") val audioBaseUrl: String,
)

@Serializable
data class AyahTimingDto(
    @SerialName("verse_key") val verseKey: String, // e.g. "1:1"
    @SerialName("timestamp_from") val timestampFrom: Long,
    @SerialName("timestamp_to") val timestampTo: Long,
    @SerialName("audio_url") val audioUrl: String? = null,
    // List of arrays: [word_index, start_ms, end_ms]
    val segments: List<List<Long>> 
)
