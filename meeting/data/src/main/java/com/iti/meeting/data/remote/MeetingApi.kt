package com.iti.meeting.data.remote

import com.iti.meeting.data.remote.MeetingEndpoints
import com.iti.data.network.dto.ApiEnvelope
import com.iti.meeting.data.remote.dto.MeetingRequestAcceptedDto
import com.iti.meeting.data.remote.dto.MeetingRequestCreatedDto
import com.iti.meeting.data.remote.dto.MeetingSheikhSummaryDto
import com.iti.meeting.data.remote.dto.SendMeetingRequestDto
import com.iti.meeting.data.remote.dto.SetAvailabilityRequestDto
import com.iti.meeting.data.remote.dto.SheikhAvailabilityDto
import com.iti.meeting.data.remote.dto.TokenRefreshDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MeetingApi(private val httpClient: HttpClient) {

    suspend fun setAvailability(status: String): SheikhAvailabilityDto =
        httpClient.put(MeetingEndpoints.InstantMeetings.SET_AVAILABILITY) {
            contentType(ContentType.Application.Json)
            setBody(SetAvailabilityRequestDto(status))
        }.body<ApiEnvelope<SheikhAvailabilityDto>>().data!!

    suspend fun getSheikhAvailability(sheikhId: String): SheikhAvailabilityDto =
        httpClient.get(MeetingEndpoints.InstantMeetings.getAvailability(sheikhId))
            .body<ApiEnvelope<SheikhAvailabilityDto>>().data!!

    suspend fun getAvailableSheikhs(): List<MeetingSheikhSummaryDto> =
        httpClient.get(MeetingEndpoints.Sheikh.ALL) {
            parameter("availability", "AVAILABLE")
        }.body<ApiEnvelope<List<MeetingSheikhSummaryDto>>>().data.orEmpty()

    suspend fun sendMeetingRequest(sheikhId: String, note: String?): MeetingRequestCreatedDto =
        httpClient.post(MeetingEndpoints.InstantMeetings.request(sheikhId)) {
            contentType(ContentType.Application.Json)
            setBody(SendMeetingRequestDto(note))
        }.body<ApiEnvelope<MeetingRequestCreatedDto>>().data!!

    suspend fun cancelMeetingRequest(requestId: String) {
        httpClient.post(MeetingEndpoints.InstantMeetings.cancel(requestId))
    }

    suspend fun acceptMeetingRequest(requestId: String): MeetingRequestAcceptedDto =
        httpClient.post(MeetingEndpoints.InstantMeetings.accept(requestId))
            .body<ApiEnvelope<MeetingRequestAcceptedDto>>().data!!

    suspend fun declineMeetingRequest(requestId: String) {
        httpClient.post(MeetingEndpoints.InstantMeetings.decline(requestId))
    }

    suspend fun endMeeting(requestId: String) {
        httpClient.post(MeetingEndpoints.InstantMeetings.end(requestId))
    }

    suspend fun refreshToken(requestId: String): TokenRefreshDto =
        httpClient.get(MeetingEndpoints.InstantMeetings.token(requestId))
            .body<ApiEnvelope<TokenRefreshDto>>().data!!
}
