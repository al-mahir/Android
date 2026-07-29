package com.iti.domain.model

data class MeetingSheikhSummary(
    val sheikhId: String,
    val name: String,
    val avatarUrl: String?,
    val status: SheikhAvailabilityStatus,
)

