package com.iti.meeting.domain.model

data class TokenRefresh(
    val token: String,
    val channelName: String,
    val userAccount: String,
)
