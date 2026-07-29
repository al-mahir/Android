package com.iti.meeting.domain.model

data class MeetingRequestAccepted(
    val status: String,
    val circleId: String,
    val channelName: String,
    val sheikhAgoraToken: String,
    val uid: Int,
)

