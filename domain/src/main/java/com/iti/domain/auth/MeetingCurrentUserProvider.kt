package com.iti.domain.auth

/**
 * The signed-in user's own id, needed to subscribe to per-user STOMP topics (e.g.
 * `/topic/students/{studentId}/meeting-requests/{requestId}`). `:meeting-kit` has no session
 * concept of its own, so the host supplies this.
 */
fun interface MeetingCurrentUserProvider {
    suspend fun currentUserId(): String?
}


