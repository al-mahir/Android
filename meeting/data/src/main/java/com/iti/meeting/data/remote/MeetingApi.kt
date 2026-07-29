package com.iti.meeting.data.remote

import com.iti.meeting.data.remote.MeetingEndpoints
import com.iti.data.network.dto.ApiEnvelope
import com.iti.meeting.data.remote.dto.DeclineMeetingRequestDto
import com.iti.meeting.data.remote.dto.MeetingRequestAcceptedDto
import com.iti.meeting.data.remote.dto.MeetingRequestCreatedDto
import com.iti.meeting.data.remote.dto.MeetingSheikhSummaryDto
import com.iti.meeting.data.remote.dto.SendMeetingRequestDto
import com.iti.meeting.data.remote.dto.SetAvailabilityRequestDto
import com.iti.meeting.data.remote.dto.SheikhAvailabilityDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MeetingApi(private val httpClient: HttpClient) {

    suspend fun setAvailability(sheikhId: String, status: String): SheikhAvailabilityDto =
        httpClient.put(MeetingEndpoints.Sheikh.availability(sheikhId)) {
            contentType(ContentType.Application.Json)
            setBody(SetAvailabilityRequestDto(status))
        }.body()

    suspend fun getAvailableSheikhs(): List<MeetingSheikhSummaryDto> =
        httpClient.get(MeetingEndpoints.Sheikh.ALL) {
            parameter("availability", "AVAILABLE")
        }.body<ApiEnvelope<List<MeetingSheikhSummaryDto>>>().data.orEmpty()

    suspend fun sendMeetingRequest(sheikhId: String, note: String?): MeetingRequestCreatedDto =
        httpClient.post(MeetingEndpoints.Sheikh.meetingRequests(sheikhId)) {
            contentType(ContentType.Application.Json)
            setBody(SendMeetingRequestDto(note))
        }.body()

    suspend fun cancelMeetingRequest(requestId: String) {
        httpClient.delete(MeetingEndpoints.MeetingRequests.byId(requestId))
    }

    suspend fun acceptMeetingRequest(requestId: String): MeetingRequestAcceptedDto =
        httpClient.post(MeetingEndpoints.MeetingRequests.accept(requestId)).body()

    suspend fun declineMeetingRequest(requestId: String, reason: String?) {
        httpClient.post(MeetingEndpoints.MeetingRequests.decline(requestId)) {
            contentType(ContentType.Application.Json)
            setBody(DeclineMeetingRequestDto(reason))
        }
    }
}





