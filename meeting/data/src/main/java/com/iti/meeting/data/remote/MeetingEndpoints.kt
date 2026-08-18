package com.iti.meeting.data.remote


object MeetingEndpoints {

    /** Qur'an Study Circles — `docs/features/20-quran-study-circles-api.md`. */
    object Circles {
        const val BASE = "api/circles"

        /** `GET/PATCH/DELETE /api/circles/{circleId}` — details / update / cancel (SCHEDULED only). */
        fun byId(circleId: String) = "$BASE/$circleId"

        /** `POST /api/circles/{circleId}/start` — SCHEDULED -> ONGOING (owner only). */
        fun start(circleId: String) = "${byId(circleId)}/start"

        /** `POST /api/circles/{circleId}/join` — body `{password}`; returns membershipId. */
        fun join(circleId: String) = "${byId(circleId)}/join"

        /** `POST /api/circles/{circleId}/leave` — user leaves an active circle. */
        fun leave(circleId: String) = "${byId(circleId)}/leave"

        /** `POST /api/circles/{circleId}/end` — ONGOING -> COMPLETED (owner only). */
        fun end(circleId: String) = "${byId(circleId)}/end"

        /** `POST /api/circles/{circleId}/approve/{userId}` — approve a pending join request (owner). */
        fun approve(circleId: String, userId: String) = "${byId(circleId)}/approve/$userId"

        /** `POST /api/circles/{circleId}/reject/{userId}` — reject a pending join request (owner). */
        fun reject(circleId: String, userId: String) = "${byId(circleId)}/reject/$userId"

        /** `DELETE /api/circles/{circleId}/members/{userId}` — owner removes a member. */
        fun removeMember(circleId: String, userId: String) = "${byId(circleId)}/members/$userId"

        /** `GET /api/circles/{circleId}/token` — Agora token (owner or active member, ONGOING only). */
        fun token(circleId: String) = "${byId(circleId)}/token"

        /** `GET /api/circles/{circleId}/pending-requests` — owner's pending join requests. */
        fun pendingRequests(circleId: String) = "${byId(circleId)}/pending-requests"

        /** `GET /api/circles/{circleId}/members` — active members (private requires membership). */
        fun members(circleId: String) = "${byId(circleId)}/members"

        /** `GET /api/circles/mine` — active circles the current user is a member of. */
        const val MINE = "$BASE/mine"

        /** `GET /api/circles/mine/private` — PRIVATE circles owned by the current user (with invite tokens). */
        const val MINE_PRIVATE = "$BASE/mine/private"

        /** `GET /api/circles/history` — all circles the user has ever been a member of. */
        const val HISTORY = "$BASE/history"

        /** `POST /api/circles/join/{token}` — join a PRIVATE circle via its invite token. */
        fun joinViaToken(token: String) = "$BASE/join/$token"
    }

    object Sheikh {
        const val ALL = "api/sheikh"
    }

    /** `POST/PUT/GET api/instant-meetings/...` — the Sheikh 1:1 instant meeting request flow. */
    object InstantMeetings {
        const val BASE = "api/instant-meetings"

        /** Student only. */
        fun request(sheikhId: String) = "$BASE/sheikh/$sheikhId/request"

        /** Sheikh only — status derived from the caller's JWT, no sheikhId in the path. */
        const val SET_AVAILABILITY = "$BASE/sheikh/availability"

        /** Public/student use. */
        fun getAvailability(sheikhId: String) = "$BASE/sheikh/$sheikhId/availability"

        fun byId(requestId: String) = "$BASE/$requestId"
        fun accept(requestId: String) = "${byId(requestId)}/accept"
        fun decline(requestId: String) = "${byId(requestId)}/decline"
        fun cancel(requestId: String) = "${byId(requestId)}/cancel"
        fun end(requestId: String) = "${byId(requestId)}/end"
        fun token(requestId: String) = "${byId(requestId)}/token"
    }
}

/** STOMP destination constants, relative to [com.iti.meeting.domain.config.MeetingKitConfig.wsBaseUrl]. */
object MeetingWsDestinations {
    const val CONNECT_PATH = "/ws"

    fun circleHost(circleId: String) = "/topic/circles/$circleId/host"
    fun circleGuest(circleId: String, guestId: String) = "/topic/circles/$circleId/guest/$guestId"

    /**
     * The single unified topic for an instant-meeting request's lifecycle
     * (REQUEST_ACCEPTED/DECLINED/CANCELLED/EXPIRED, MEETING_ENDED once accepted). Both the
     * requesting student and the responding sheikh subscribe to the same destination once a
     * requestId exists.
     */
    fun meetingRequest(requestId: String) = "/topic/meeting-requests/$requestId"

    /**
     * Topic for the sheikh to learn about brand-new incoming requests before a requestId exists
     * client-side.
     */
    fun sheikhRequests(sheikhId: String) = "/topic/sheikhs/$sheikhId/requests"

    /** Roster + lifecycle for a circle's members and owner (`MEMBER_JOINED/LEFT/REMOVED`, `CIRCLE_STARTED/ENDED/CANCELLED`). */
    fun circle(circleId: String) = "/topic/circles/$circleId"

    /** Live pending join requests, delivered to the circle owner (`CIRCLE_JOIN_REQUEST_RECEIVED/REMOVED`). */
    fun circleRequests(circleId: String) = "/topic/circles/$circleId/requests"

    /** The requesting user's own join-request decision (`REQUEST_APPROVED/REJECTED`). */
    fun circleMembership(membershipId: String) = "/topic/circle-memberships/$membershipId"
}
