package com.iti.domain.auth.model

data class User(
    val id: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String? = null
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
