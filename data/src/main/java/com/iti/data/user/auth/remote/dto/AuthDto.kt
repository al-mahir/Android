package com.iti.data.user.auth.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val gender: String? = null,
    val profilePictureUrl: String? = null,
    val provider: String? = null,
    val roles: List<String> = emptyList(),
)

@Serializable
data class AuthDataDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isNewUser: Boolean? = null,
    val user: UserDto? = null,
)
