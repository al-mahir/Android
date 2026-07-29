package com.iti.meeting.data.repository

import com.iti.domain.model.MeetingSheikhSummary
import com.iti.domain.model.SheikhAvailabilityStatus
import com.iti.meeting.domain.model.MeetingRequestAccepted
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.meeting.domain.repository.SendMeetingRequestResult
import com.iti.meeting.data.remote.dto.MeetingRequestAcceptedDto
import com.iti.meeting.data.remote.dto.MeetingSheikhSummaryDto
import com.iti.meeting.data.remote.MeetingApi
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

import com.iti.meeting.domain.repository.MeetingRequestEvent
import com.iti.meeting.domain.repository.IncomingRequestEvent
import com.iti.meeting.data.realtime.StompClient
import com.iti.meeting.data.remote.MeetingWsDestinations
import com.iti.meeting.data.remote.MeetingKitJson
import com.iti.meeting.data.realtime.SocketEventEnvelope
import com.iti.meeting.data.remote.dto.RequestAcceptedEventDto
import com.iti.meeting.data.remote.dto.RequestDeclinedEventDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.decodeFromJsonElement

class MeetingRepositoryImpl(
    private val api: MeetingApi,
    private val stompClient: StompClient,
) : MeetingRepository {

    override suspend fun getAvailableSheikhs(): Result<List<MeetingSheikhSummary>> = runCatching {
        api.getAvailableSheikhs().map { it.toDomain() }
    }

    override suspend fun setMyAvailability(sheikhId: String, status: SheikhAvailabilityStatus): Result<Unit> =
        runCatching { api.setAvailability(sheikhId, status.name) }.map { }

    override suspend fun sendMeetingRequest(sheikhId: String, note: String?): SendMeetingRequestResult = try {
        val created = api.sendMeetingRequest(sheikhId, note)
        SendMeetingRequestResult.Pending(created.requestId, created.expiresAt)
    } catch (e: ClientRequestException) {
        when (e.response.status) {
            HttpStatusCode.Conflict -> SendMeetingRequestResult.SheikhUnavailable
            HttpStatusCode.NotFound -> SendMeetingRequestResult.SheikhNotFound
            else -> SendMeetingRequestResult.Error(e.message)
        }
    } catch (e: Exception) {
        SendMeetingRequestResult.Error(e.message ?: "Request failed")
    }

    override suspend fun cancelMeetingRequest(requestId: String): Result<Unit> =
        runCatching { api.cancelMeetingRequest(requestId) }

    override suspend fun acceptMeetingRequest(requestId: String): Result<MeetingRequestAccepted> =
        runCatching { api.acceptMeetingRequest(requestId).toDomain() }

    override suspend fun declineMeetingRequest(requestId: String, reason: String?): Result<Unit> =
        runCatching { api.declineMeetingRequest(requestId, reason) }

    override fun observeMeetingRequestEvents(studentId: String, requestId: String): Flow<MeetingRequestEvent> {
        return stompClient.subscribe(MeetingWsDestinations.studentMeetingRequest(studentId, requestId))
            .onStart { stompClient.connect() }
            .mapNotNull { frame ->
                val envelope = runCatching {
                    MeetingKitJson.decodeFromString(SocketEventEnvelope.serializer(), frame.body)
                }.getOrNull() ?: return@mapNotNull null

                when (envelope.eventType) {
                    "REQUEST_ACCEPTED" -> {
                        val payload = runCatching {
                            MeetingKitJson.decodeFromJsonElement(RequestAcceptedEventDto.serializer(), envelope.payload)
                        }.getOrNull() ?: return@mapNotNull null
                        MeetingRequestEvent.Accepted(payload.circleId, payload.agoraToken, payload.channelName, payload.uid)
                    }
                    "REQUEST_DECLINED" -> {
                        val payload = runCatching {
                            MeetingKitJson.decodeFromJsonElement(RequestDeclinedEventDto.serializer(), envelope.payload)
                        }.getOrNull()
                        MeetingRequestEvent.Declined(payload?.reason)
                    }
                    "REQUEST_EXPIRED" -> MeetingRequestEvent.Expired
                    else -> null
                }
            }
    }

    override fun observeIncomingRequests(sheikhId: String): Flow<com.iti.meeting.domain.repository.IncomingRequestEvent> {
        return stompClient.subscribe(MeetingWsDestinations.sheikhRequests(sheikhId))
            .onStart { stompClient.connect() }
            .mapNotNull { frame ->
                val envelope = runCatching {
                    MeetingKitJson.decodeFromString(SocketEventEnvelope.serializer(), frame.body)
                }.getOrNull() ?: return@mapNotNull null

                when (envelope.eventType) {
                    "SHEIKH_MEETING_REQUEST_RECEIVED" -> {
                        val payload = runCatching {
                            MeetingKitJson.decodeFromJsonElement(
                                com.iti.meeting.data.remote.dto.SheikhMeetingRequestReceivedDto.serializer(),
                                envelope.payload,
                            )
                        }.getOrNull() ?: return@mapNotNull null
                        com.iti.meeting.domain.repository.IncomingRequestEvent.Received(
                            requestId = payload.requestId,
                            studentName = payload.studentName.orEmpty(),
                            note = payload.note.orEmpty(),
                            expiresAt = payload.expiresAt,
                        )
                    }
                    "REQUEST_CANCELLED" -> {
                        IncomingRequestEvent.Cancelled("")
                    }
                    else -> null
                }
            }
    }
}

private fun MeetingSheikhSummaryDto.toDomain(): MeetingSheikhSummary {
    val displayName = name
        ?: listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { username ?: id }
    val status = sheikhStatus
        ?.let { raw -> runCatching { SheikhAvailabilityStatus.valueOf(raw) }.getOrNull() }
        ?: SheikhAvailabilityStatus.OFFLINE
    return MeetingSheikhSummary(
        sheikhId = id,
        name = displayName,
        avatarUrl = profilePictureUrl,
        status = status,
    )
}

private fun MeetingRequestAcceptedDto.toDomain(): MeetingRequestAccepted = MeetingRequestAccepted(
    status = status,
    circleId = circleId,
    channelName = channelName,
    sheikhAgoraToken = sheikhAgoraToken,
    uid = uid
)





