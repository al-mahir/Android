package com.iti.data.mapper

import com.iti.data.dto.SheikhDto
import com.iti.data.dto.sheikh.SheikhApiDto
import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability

// ── Legacy mapper (still used by AlmahirFakeDataSource / AlmahirRepositoryImpl) ──

internal fun SheikhDto.toDomain(): Sheikh = Sheikh(
    id = id,
    name = name,
    initials = name.toInitials(),
    avatarUrl = avatarUrl,
    rating = rating,
    reviewCount = reviewCount,
    availability = availability.toSheikhAvailability(),
    specialization = specialization,
    bio = bio,
    activeCircleCount = activeCircleCount,
    totalStudents = totalStudents,
)

// ── Real-API mapper ──

internal fun SheikhApiDto.toDomain(): Sheikh {
    val fullName = name
        ?: listOfNotNull(firstName, lastName).joinToString(" ").trim().ifEmpty { username ?: id }

    return Sheikh(
        id = id,
        name = fullName,
        initials = fullName.toInitials(),
        avatarUrl = profilePictureUrl,
        rating = rating ?: 0.0,
        reviewCount = reviewCount ?: 0,
        availability = sheikhStatus.toSheikhAvailability(),
        specialization = specialization ?: "",
        bio = bio ?: "",
        activeCircleCount = activeCircleCount ?: 0,
        totalStudents = totalStudents ?: 0,
    )
}

// ── Shared helpers ──

/** Maps a nullable status string to a domain SheikhAvailability. */
private fun String?.toSheikhAvailability(): SheikhAvailability = when (this?.lowercase()) {
    "available" -> SheikhAvailability.AVAILABLE
    "in_session", "in-session", "busy" -> SheikhAvailability.IN_SESSION
    else -> SheikhAvailability.OFFLINE
}
