package com.iti.data.mapper

import com.iti.data.dto.StudyCircleDto
import com.iti.domain.model.StudyCircle

internal fun StudyCircleDto.toDomain(): StudyCircle = StudyCircle(
    id = id,
    title = title,
    hostName = hostName,
    isJoined = isJoined,
)
