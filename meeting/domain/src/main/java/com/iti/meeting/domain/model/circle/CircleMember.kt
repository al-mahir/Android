package com.iti.meeting.domain.model.circle

enum class CircleMemberRole {
    HOST,
    MEMBER,
}

/** A user's membership in a circle. [id] is the membership UUID used by the join-request
 * lifecycle topics (`/topic/circle-memberships/{membershipId}`). */
data class CircleMember(
    val id: String,
    val userId: String,
    val displayName: String,
    val initials: String,
    val avatarUrl: String? = null,
    val role: CircleMemberRole = CircleMemberRole.MEMBER,
)
