package com.iti.meeting.domain.model

data class MeetingRequestAccepted(
    val status: String,
    val requestId: String,
    val channelName: String,
    val agoraToken: String,
    val userAccount: String,
)
