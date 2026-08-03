package com.iti.meeting.domain.model

data class PendingMeetingRequest(
    val requestId: String,
    val sheikhId: String,
    val sheikhName: String?,
    val expiresAt: String,
)
