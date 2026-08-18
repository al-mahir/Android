package com.iti.meeting.domain.model.circle

/** A join request awaiting the circle owner's decision (`/topic/circles/{circleId}/requests`,
 * `CIRCLE_JOIN_REQUEST_RECEIVED`). */
data class PendingJoinRequest(
    val membershipId: String,
    val userId: String,
    val displayName: String,
    val initials: String,
    val avatarUrl: String? = null,
    val joinedAt: String = "",
)
