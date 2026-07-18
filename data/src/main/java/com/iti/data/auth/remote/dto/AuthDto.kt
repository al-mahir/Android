package com.iti.data.auth.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String? = null
)

@Serializable
data class AuthDataDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isNewUser: Boolean? = null,
    val user: UserDto? = null
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val fieldErrors: Map<String, String>? = null
)
