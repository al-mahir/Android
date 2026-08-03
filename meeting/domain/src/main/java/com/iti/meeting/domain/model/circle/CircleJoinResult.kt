package com.iti.meeting.domain.model.circle

enum class CircleJoinError {
    CIRCLE_FULL,
    TIME_CONFLICT,
    INVALID_PASSWORD,
    UNKNOWN,
}

/** Outcome of `POST /api/circles/{circleId}/join` — the caller must branch on whether the user
 * was admitted immediately (`Joined`) or must wait for the owner's approval (`PendingApproval`). */
sealed interface CircleJoinResult {
    data class Joined(val membershipId: String) : CircleJoinResult
    data class PendingApproval(val membershipId: String) : CircleJoinResult
    data class Error(val error: CircleJoinError, val message: String) : CircleJoinResult
}
