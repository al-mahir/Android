package com.example.mushaf.data.recitation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReciterDto(
    val id: Int,
    val name: String,
    @SerialName("name_arabic") val nameArabic: String,
    val style: String, 
    @SerialName("audio_base_url") val audioBaseUrl: String,
)

@Serializable
data class AyahTimingDto(
    @SerialName("verse_key") val verseKey: String, 
    @SerialName("timestamp_from") val timestampFrom: Long,
    @SerialName("timestamp_to") val timestampTo: Long,
    
    val segments: List<List<Long>> 
)
