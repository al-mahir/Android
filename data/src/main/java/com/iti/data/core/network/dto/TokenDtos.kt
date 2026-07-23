package com.iti.data.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

@Serializable
data class TokenPairDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
)
