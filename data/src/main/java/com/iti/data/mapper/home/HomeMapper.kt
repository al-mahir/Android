package com.iti.data.mapper.home

import com.iti.data.dto.home.ContinueReadingDto
import com.iti.data.dto.home.HomeSummaryDto
import com.iti.data.dto.home.SheikhDto
import com.iti.data.dto.home.StudyCircleDto
import com.iti.data.dto.home.UserDto
import com.iti.domain.model.home.ContinueReading
import com.iti.domain.model.home.HomeSummary
import com.iti.domain.model.home.Sheikh
import com.iti.domain.model.home.SheikhAvailability
import com.iti.domain.model.home.StudyCircle
import com.iti.domain.model.home.UserSummary

/** DTO -> domain mapping. The only place wire shapes are allowed to meet domain models. */
internal fun HomeSummaryDto.toDomain(): HomeSummary = HomeSummary(
    user = user.toDomain(),
    continueReading = continueReading?.toDomain(),
    sheikhs = sheikhs.map(SheikhDto::toDomain),
    circles = circles.map(StudyCircleDto::toDomain),
)

internal fun UserDto.toDomain(): UserSummary = UserSummary(
    displayName = displayName,
    initials = displayName.toInitials(),
    avatarUrl = avatarUrl,
)

internal fun ContinueReadingDto.toDomain(): ContinueReading = ContinueReading(
    surahName = surahName,
    ayahNumber = ayahNumber,
    pageNumber = pageNumber,
)

internal fun SheikhDto.toDomain(): Sheikh = Sheikh(
    id = id,
    name = name,
    initials = name.toInitials(),
    avatarUrl = avatarUrl,
    rating = rating,
    availability = availability.toAvailability(),
)

internal fun StudyCircleDto.toDomain(): StudyCircle = StudyCircle(
    id = id,
    title = title,
    hostName = hostName,
    isJoined = isJoined,
)

/** Unknown tokens degrade to [SheikhAvailability.OFFLINE] rather than crashing the screen. */
private fun String.toAvailability(): SheikhAvailability = when (lowercase()) {
    "available" -> SheikhAvailability.AVAILABLE
    "in_session" -> SheikhAvailability.IN_SESSION
    else -> SheikhAvailability.OFFLINE
}

/** Titles that carry no identity, so they must not contribute a letter to the avatar. */
private val HONORIFICS = setOf(
    "الشيخ",
    "الشيخة",
    "الأستاذ",
    "الأستاذة",
    "الدكتور",
    "الدكتورة",
)

/**
 * Avatar initials. A two-word name gives one letter each ("Jamal Darwish" -> "JD"); a name
 * left with a single word after dropping the honorific gives its first two letters
 * ("الشيخ أحمد" -> "أح"), which is what the design shows.
 */
private fun String.toInitials(): String {
    val words = trim()
        .split(' ', '\u00A0')
        .filter { it.isNotBlank() }
        .filterNot { it in HONORIFICS }

    return when (words.size) {
        0 -> ""
        1 -> words.first().take(2)
        else -> words.take(2).mapNotNull { word -> word.firstOrNull() }.joinToString(separator = "")
    }
}
