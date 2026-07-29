package com.iti.domain.model

data class StudyCircle(
    val id: String,
    val surahName: String,
    val hostId: String,
    val hostName: String,
    val hostInitials: String,
    val isLive: Boolean,
    val difficulty: CircleDifficulty,
    val participantCount: Int,
    val maxParticipants: Int,
    val currentActivity: String,
    val isJoined: Boolean,
    val isWaitingApproval: Boolean = false,
)

enum class CircleDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
}
