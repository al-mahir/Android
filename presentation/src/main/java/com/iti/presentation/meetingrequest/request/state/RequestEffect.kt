package com.iti.presentation.meetingrequest.request


sealed interface RequestEffect {
    data class MeetingAccepted(val circleId: String, val agoraToken: String, val channelName: String, val uid: Int) : RequestEffect
    data class ShowMessage(val message: String) : RequestEffect
}


