package com.iti.domain.model

data class MeetingStatus(
    val id: String,
    val userId: String,
    val meetingTime: Long,
    val status: String // e.g., "SCHEDULED", "COMPLETED", "CANCELLED"
)
