package com.iti.data.dto.home

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the Home payload. These mirror the shape the backend will return and are
 * confined to `:data` — the UI only ever sees the mapped `domain` models.
 */
@Serializable
data class HomeSummaryDto(
    @SerialName("user") val user: UserDto,
    @SerialName("continue_reading") val continueReading: ContinueReadingDto? = null,
    @SerialName("sheikhs") val sheikhs: List<SheikhDto> = emptyList(),
    @SerialName("circles") val circles: List<StudyCircleDto> = emptyList(),
)

@Serializable
data class UserDto(
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class ContinueReadingDto(
    @SerialName("surah_name") val surahName: String,
    @SerialName("ayah_number") val ayahNumber: Int,
    @SerialName("page_number") val pageNumber: Int,
)

@Serializable
data class SheikhDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("rating") val rating: Double,
    /** Raw backend token: `available`, `in_session`, `offline`. Unknown values map to offline. */
    @SerialName("availability") val availability: String,
)

@Serializable
data class StudyCircleDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("host_name") val hostName: String,
    @SerialName("is_joined") val isJoined: Boolean = false,
)
