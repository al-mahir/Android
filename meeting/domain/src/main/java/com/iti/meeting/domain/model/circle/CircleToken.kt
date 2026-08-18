package com.iti.meeting.domain.model.circle

/** Agora credentials for joining a circle's audio/video channel — only issued while the circle is ONGOING. */
data class CircleToken(
    val token: String,
    val channelName: String,
    val userAccount: String,
)
