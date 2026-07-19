package com.iti.data.mapper

import com.iti.data.dto.SheikhDto
import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability

internal fun SheikhDto.toDomain(): Sheikh = Sheikh(
    id = id,
    name = name,
    initials = name.toInitials(),
    avatarUrl = avatarUrl,
    rating = rating,
    availability = availability.toAvailability(),
)

private fun String.toAvailability(): SheikhAvailability = when (lowercase()) {
    "available" -> SheikhAvailability.AVAILABLE
    "in_session" -> SheikhAvailability.IN_SESSION
    else -> SheikhAvailability.OFFLINE
}
