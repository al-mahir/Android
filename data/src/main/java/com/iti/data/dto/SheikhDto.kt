package com.iti.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SheikhDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("rating") val rating: Double,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("availability") val availability: String,
    @SerialName("specialization") val specialization: String = "",
    @SerialName("bio") val bio: String = "",
    @SerialName("active_circle_count") val activeCircleCount: Int = 0,
    @SerialName("total_students") val totalStudents: Int = 0,
)
