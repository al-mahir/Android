package com.iti.data.mapper

import com.iti.data.dto.StudyCircleDto
import com.iti.domain.model.CircleDifficulty
import com.iti.domain.model.StudyCircle

internal fun StudyCircleDto.toDomain(): StudyCircle = StudyCircle(
    id = id,
    surahName = surahName,
    hostId = hostId,
    hostName = hostName,
    hostInitials = hostInitials,
    isLive = isLive,
    difficulty = difficulty.toDifficulty(),
    participantCount = participantCount,
    maxParticipants = maxParticipants,
    currentActivity = currentActivity,
    isJoined = isJoined,
    isWaitingApproval = isWaitingApproval,
)

private fun String.toDifficulty(): CircleDifficulty = when (lowercase()) {
    "intermediate" -> CircleDifficulty.INTERMEDIATE
    "advanced" -> CircleDifficulty.ADVANCED
    else -> CircleDifficulty.BEGINNER
}
