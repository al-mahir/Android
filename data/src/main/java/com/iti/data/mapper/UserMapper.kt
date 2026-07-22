package com.iti.data.mapper

import com.iti.data.dto.UserDto
import com.iti.domain.model.User

internal fun UserDto.toDomain(): User = User(
    id = id,
    displayName = displayName,
    initials = displayName.toInitials(),
    avatarUrl = avatarUrl,
    email = email,
    joinedAtEpochMillis = joinedAtEpochMillis,
)
