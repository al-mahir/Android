package com.iti.domain.auth.model

data class User(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String? = null,
    val gender: String? = null,
    val profilePictureUrl: String? = null,
    val provider: String? = null,
    val roles: List<String> = emptyList(),
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)

data class AuthData(
    val tokens: AuthTokens,
    val user: User,
    val isNewUser: Boolean = false
)

