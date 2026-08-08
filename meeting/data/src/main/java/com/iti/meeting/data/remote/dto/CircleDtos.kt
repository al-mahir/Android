package com.iti.meeting.data.remote.dto

import kotlinx.serialization.Serializable

/** Request bodies and responses for the Qur'an Study Circles API — see
 * `docs/features/20-quran-study-circles-api.md`. Field names follow the documented camelCase
 * contract; unknown keys are ignored by `MeetingKitJson`. */

@Serializable
data class CreateCircleRequestDto(
    val name: String,
    val startDate: String,
    val endDate: String? = null,
    val type: String = "PUBLIC",
    val requiresApproval: Boolean = false,
    val maxParticipants: Int = 10,
    val password: String? = null,
)

@Serializable
data class JoinCircleRequestDto(val password: String? = null)

@Serializable
data class CircleDto(
    val id: String = "",
    val name: String = "",
    val circleId: String = "",
    val title: String = "",
    val startDate: String = "",
    val endDate: String? = null,
    val type: String = "PUBLIC",
    val status: String = "SCHEDULED",
    val requiresApproval: Boolean = false,
    val maxParticipants: Int = 10,
    val currentMembers: Int = 0,
    val memberCount: Int = 0,
    val host: CircleMemberDto? = null,
)

@Serializable
data class CircleMemberDto(
    val id: String,
    val userId: String,
    val displayName: String = "",
    val initials: String = "",
    val avatarUrl: String? = null,
    val role: String = "MEMBER",
)

/** The `joinCircle` success body — the membership object, keyed by `membershipId`. */
@Serializable
data class JoinCircleResponseDto(
    val membershipId: String,
    val status: String? = null,
    val message: String? = null,
)

/** `GET /api/circles/{circleId}/pending-requests` item and the `CIRCLE_JOIN_REQUEST_RECEIVED` payload. */
@Serializable
data class PendingJoinRequestDto(
    val membershipId: String,
    val userId: String,
    val displayName: String = "",
    val initials: String = "",
    val avatarUrl: String? = null,
    val joinedAt: String = "",
)

@Serializable
data class CircleTokenDto(
    val token: String,
    val channelName: String,
    val userAccount: String,
)

/** `POST /api/circles/{circleId}/end` — status + endedAt. */
@Serializable
data class EndCircleResponseDto(
    val status: String? = null,
    val endedAt: String? = null,
)

/** 4xx error body — decoded to distinguish join failures that share a status code (e.g. 409). */
@Serializable
data class CircleErrorResponseDto(
    val error: String? = null,
    val message: String? = null,
)

/** The `REQUEST_REJECTED` payload when it is not a bare reason string. */
@Serializable
data class RejectReasonDto(
    val reason: String? = null,
    val message: String? = null,
)
