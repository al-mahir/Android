package com.iti.meeting.data.remote


object MeetingEndpoints {

    object Circles {
        const val BASE = "api/v1/circles"
        fun byId(circleId: String) = "$BASE/$circleId"
        fun joinRequests(circleId: String) = "${byId(circleId)}/join-requests"
        fun approve(circleId: String) = "${byId(circleId)}/approve"
        fun reject(circleId: String) = "${byId(circleId)}/reject"
        fun participants(circleId: String) = "${byId(circleId)}/participants"
        fun leave(circleId: String) = "${byId(circleId)}/leave"
        fun end(circleId: String) = "${byId(circleId)}/end"
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
}
