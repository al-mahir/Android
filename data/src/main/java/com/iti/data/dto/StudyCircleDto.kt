package com.iti.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudyCircleDto(
    @SerialName("id") val id: String,
    @SerialName("surah_name") val surahName: String,
    @SerialName("host_id") val hostId: String,
    @SerialName("host_name") val hostName: String,
    @SerialName("host_initials") val hostInitials: String,
    @SerialName("is_live") val isLive: Boolean = true,
    @SerialName("difficulty") val difficulty: String = "beginner",
    @SerialName("participant_count") val participantCount: Int = 0,
    @SerialName("max_participants") val maxParticipants: Int = 20,
    @SerialName("current_activity") val currentActivity: String = "Reading",
    @SerialName("is_joined") val isJoined: Boolean = false,
    @SerialName("is_waiting_approval") val isWaitingApproval: Boolean = false,
)
