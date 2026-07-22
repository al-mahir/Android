package com.example.mushaf.data.recitation.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuranApiVersesResponse(
    val verses: List<QuranApiVerse>
)

@Serializable
data class QuranApiVerse(
    val id: Int,
    @SerialName("verse_number") val verseNumber: Int,
    @SerialName("verse_key") val verseKey: String,
    val audio: QuranApiAudio? = null
)

@Serializable
data class QuranApiAudio(
    val url: String? = null,
    val segments: List<List<Long>> = emptyList()
)
