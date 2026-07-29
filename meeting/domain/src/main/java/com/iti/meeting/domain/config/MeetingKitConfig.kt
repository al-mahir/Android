package com.iti.meeting.domain.config



data class MeetingKitConfig(
    val restBaseUrl: String,
    val wsBaseUrl: String,
    val agoraAppId: String,
    val enableHttpLogging: Boolean,
)



