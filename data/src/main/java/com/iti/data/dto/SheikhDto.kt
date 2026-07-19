package com.iti.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SheikhDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("rating") val rating: Double,
    /** Raw backend token: `available`, `in_session`, `offline`. Unknown values map to offline. */
    @SerialName("availability") val availability: String,
)
