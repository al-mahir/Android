package com.iti.meeting.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendMeetingRequestDto(val note: String? = null)

@Serializable
data class MeetingRequestCreatedDto(
    val requestId: String,
    val status: String = "PENDING",
    val channelName: String,
    val expiresAt: String = "2099-12-31T23:59:59Z",
)

@Serializable
data class MeetingRequestAcceptedDto(
    val status: String,
    val requestId: String,
    val channelName: String,
    val agoraToken: String,
    val userAccount: String,
)

@Serializable
data class TokenRefreshDto(
    val token: String,
    val channelName: String,
    val userAccount: String,
)




@Serializable
data class SheikhMeetingRequestReceivedDto(
    val requestId: String,
    val studentId: String? = null,
    val studentName: String? = null,
    val note: String? = null,
    val expiresAt: String,
)

@Serializable
data class RequestCancelledDto(val requestId: String)

@Serializable
data class RequestAcceptedEventDto(
    val channelName: String,
    val agoraToken: String,
    val userAccount: String,
)

@Serializable
data class RequestDeclinedEventDto(val reason: String? = null)




@Serializable
data class MeetingSheikhSummaryDto(
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val username: String? = null,
    val name: String? = null,
    val profilePictureUrl: String? = null,
    val sheikhStatus: String? = null,
)

@Serializable
data class SetAvailabilityRequestDto(val status: String)

@Serializable
data class SheikhAvailabilityDto(
    val sheikhId: String,
    val status: String,
    val updatedAt: String? = null,
)
