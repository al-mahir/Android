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
        fun availability(sheikhId: String) = "api/sheikh/$sheikhId/availability"
        fun meetingRequests(sheikhId: String) = "api/sheikh/$sheikhId/meeting-requests"
    }

    object MeetingRequests {
        fun byId(requestId: String) = "api/meeting-requests/$requestId"
        fun accept(requestId: String) = "${byId(requestId)}/accept"
        fun decline(requestId: String) = "${byId(requestId)}/decline"
    }
}

/** STOMP destination constants, relative to [com.iti.meeting.domain.config.MeetingKitConfig.wsBaseUrl]. */
object MeetingWsDestinations {
    const val CONNECT_PATH = "/ws"

    fun circleHost(circleId: String) = "/topic/circles/$circleId/host"
    fun circleGuest(circleId: String, guestId: String) = "/topic/circles/$circleId/guest/$guestId"
    fun sheikhRequests(sheikhId: String) = "/topic/sheikhs/$sheikhId/requests"
    fun sheikhStatus(sheikhId: String) = "/topic/sheikhs/$sheikhId/status"
    fun studentMeetingRequest(studentId: String, requestId: String) =
        "/topic/students/$studentId/meeting-requests/$requestId"
}






