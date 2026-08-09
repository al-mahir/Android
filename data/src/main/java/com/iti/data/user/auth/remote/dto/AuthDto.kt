package com.iti.data.user.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id") val id: String = "",
    @SerialName("_id") val mongoId: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("display_name") val snakeDisplayName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
    @SerialName("phone_number") val snakePhoneNumber: String? = null,
    @SerialName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("avatar_url") val snakeAvatarUrl: String? = null,
    @SerialName("provider") val provider: String? = null,
    @SerialName("roles") val roles: List<String> = emptyList(),
)

@Serializable
data class AuthDataDto(
    @SerialName("accessToken") val accessToken: String? = null,
    @SerialName("refreshToken") val refreshToken: String? = null,
    @SerialName("token") val token: String? = null,
    @SerialName("isNewUser") val isNewUser: Boolean? = null,
    @SerialName("user") val user: UserDto? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("_id") val mongoId: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("display_name") val snakeDisplayName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
    @SerialName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("roles") val roles: List<String> = emptyList(),
)

