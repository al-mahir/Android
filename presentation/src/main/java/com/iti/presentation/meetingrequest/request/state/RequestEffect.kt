package com.iti.presentation.meetingrequest.request


sealed interface RequestEffect {
    data class MeetingAccepted(val requestId: String, val agoraToken: String, val channelName: String, val userAccount: String) : RequestEffect
    data class ShowMessage(val message: String) : RequestEffect
}
