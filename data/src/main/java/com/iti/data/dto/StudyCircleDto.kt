package com.iti.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudyCircleDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("host_name") val hostName: String,
    @SerialName("is_joined") val isJoined: Boolean = false,
)
