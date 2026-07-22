package com.iti.domain.model

data class User(
    val id: String,
    val displayName: String,
    val initials: String,
    val avatarUrl: String?,
    val email: String,
    val joinedAtEpochMillis: Long,
)
