package com.iti.meeting.domain.model

import com.iti.domain.model.SheikhAvailabilityStatus

data class SheikhAvailability(
    val sheikhId: String,
    val status: SheikhAvailabilityStatus,
    val updatedAt: String?,
)
