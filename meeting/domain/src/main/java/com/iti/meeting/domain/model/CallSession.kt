package com.iti.meeting.domain.model

data class CallSession(
    val circleId: String,
    val token: String,
    val channelName: String,
    val uid: Int,
)

