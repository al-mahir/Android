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

/** `PATCH /api/circles/{circleId}` — only non-null fields are updated. */
@Serializable
data class UpdateCircleRequestDto(
    val name: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
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
    /** Agora channel name — present in most response shapes. */
    val channelName: String? = null,
    /** UUID of the user who created the circle. */
    val ownerId: String? = null,
    /** Returned for PRIVATE circles owned by the caller; used to build invite links. */
    val inviteToken: String? = null,
)

/** Active member response — Swagger: `{id, username, status, joinedAt}`.
 * Legacy shapes may use `displayName`/`initials`/`role`; both are accepted. */
@Serializable
data class CircleMemberDto(
    val id: String = "",
    /** Caller-visible username (Swagger: `username`). */
    val username: String = "",
    /** Legacy display name, kept for back-compat with earlier response revisions. */
    val displayName: String = "",
    val initials: String = "",
    val avatarUrl: String? = null,
    val role: String = "MEMBER",
    /** Membership status, e.g. `ACTIVE`. */
    val status: String = "ACTIVE",
    /** ISO-8601 timestamp when the user was admitted to the circle. */
    val joinedAt: String = "",
    /** Legacy userId field; some shapes embed the UUID under this key. */
    val userId: String = "",
)

/** The `joinCircle` success body — the membership object, keyed by `membershipId`. */
@Serializable
data class JoinCircleResponseDto(
    val membershipId: String,
    val status: String? = null,
    val message: String? = null,
)

/** `GET /api/circles/{circleId}/pending-requests` item and the `CIRCLE_JOIN_REQUEST_RECEIVED` payload.
 * Swagger: `{userId, username, requestedAt}`. Legacy shapes may use `displayName`/`joinedAt`. */
@Serializable
data class PendingJoinRequestDto(
    val membershipId: String = "",
    val userId: String = "",
    /** Caller-visible username (Swagger: `username`). */
    val username: String = "",
    /** Legacy display name, kept for back-compat. */
    val displayName: String = "",
    val initials: String = "",
    val avatarUrl: String? = null,
    /** Swagger field name for the request timestamp. */
    val requestedAt: String = "",
    /** Legacy field name — kept for back-compat. */
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
