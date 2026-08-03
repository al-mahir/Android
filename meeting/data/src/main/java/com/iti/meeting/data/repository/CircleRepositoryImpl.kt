package com.iti.meeting.data.repository

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleMemberRole
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleToken
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.meeting.domain.model.circle.CircleJoinError
import com.iti.meeting.domain.model.circle.CircleJoinResult
import com.iti.meeting.domain.model.circle.CreateCircleRequest
import com.iti.meeting.domain.model.circle.PendingJoinRequest
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.meeting.domain.repository.CircleRosterEvent
import com.iti.meeting.domain.repository.JoinRequestEvent
import com.iti.meeting.domain.repository.PendingJoinRequestEvent
import com.iti.meeting.data.remote.CircleApi
import com.iti.meeting.data.remote.MeetingKitJson
import com.iti.meeting.data.remote.MeetingWsDestinations
import com.iti.meeting.data.remote.dto.CircleDto
import com.iti.meeting.data.remote.dto.CircleErrorResponseDto
import com.iti.meeting.data.remote.dto.CircleTokenDto
import com.iti.meeting.data.remote.dto.CircleMemberDto
import com.iti.meeting.data.remote.dto.PendingJoinRequestDto
import com.iti.meeting.data.remote.dto.RejectReasonDto
import com.iti.meeting.data.realtime.SocketEventEnvelope
import com.iti.meeting.data.realtime.StompClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

class CircleRepositoryImpl(
    private val api: CircleApi,
    private val stompClient: StompClient,
) : CircleRepository {

    override suspend fun getPublicCircles(status: CircleStatus?): Result<List<Circle>> =
        runCatching { api.getPublicCircles(status?.name).map { it.toDomain() } }

    override suspend fun getMyCircles(): Result<List<Circle>> =
        runCatching { api.getMyCircles().map { it.toDomain() } }

    override suspend fun getCircle(circleId: String): Result<Circle> =
        runCatching { api.getCircle(circleId).toDomain() }

    override suspend fun createCircle(request: CreateCircleRequest): Result<Circle> =
        runCatching { api.createCircle(request.toDto()).toDomain() }

    override suspend fun joinCircle(circleId: String, password: String?): CircleJoinResult = try {
        val response = api.joinCircle(circleId, password)
        val status = response.status.orEmpty().uppercase()
        if (status.contains("PENDING")) {
            CircleJoinResult.PendingApproval(response.membershipId)
        } else {
            CircleJoinResult.Joined(response.membershipId)
        }
    } catch (e: ClientRequestException) {
        when (e.response.status) {
            HttpStatusCode.Conflict -> {
                val body = runCatching {
                    MeetingKitJson.decodeFromString(CircleErrorResponseDto.serializer(), e.response.bodyAsText())
                }.getOrNull()
                val error = when (body?.error?.uppercase()) {
                    "CIRCLE_FULL" -> CircleJoinError.CIRCLE_FULL
                    "TIME_CONFLICT", "TIME_OVERLAP" -> CircleJoinError.TIME_CONFLICT
                    "INVALID_PASSWORD", "BAD_PASSWORD" -> CircleJoinError.INVALID_PASSWORD
                    else -> CircleJoinError.UNKNOWN
                }
                CircleJoinResult.Error(error, body?.message ?: "Join failed")
            }
            HttpStatusCode.BadRequest -> CircleJoinResult.Error(
                CircleJoinError.INVALID_PASSWORD,
                "Invalid password",
            )
            else -> CircleJoinResult.Error(CircleJoinError.UNKNOWN, e.message ?: "Join failed")
        }
    } catch (e: Exception) {
        CircleJoinResult.Error(CircleJoinError.UNKNOWN, e.message ?: "Join failed")
    }

    override suspend fun cancelJoinRequest(circleId: String): Result<Unit> =
        runCatching { api.leaveCircle(circleId) }

    override suspend fun approveJoinRequest(circleId: String, userId: String): Result<Unit> =
        runCatching { api.approveJoinRequest(circleId, userId) }

    override suspend fun rejectJoinRequest(circleId: String, userId: String): Result<Unit> =
        runCatching { api.rejectJoinRequest(circleId, userId) }

    override suspend fun getPendingRequests(circleId: String): Result<List<PendingJoinRequest>> =
        runCatching { api.getPendingRequests(circleId).map { it.toDomain() } }

    override suspend fun getMembers(circleId: String): Result<List<CircleMember>> =
        runCatching { api.getMembers(circleId).map { it.toDomain() } }

    override suspend fun removeMember(circleId: String, userId: String): Result<Unit> =
        runCatching { api.removeMember(circleId, userId) }

    override suspend fun startCircle(circleId: String): Result<Circle> =
        runCatching { api.startCircle(circleId).toDomain() }

    override suspend fun endCircle(circleId: String): Result<Unit> =
        runCatching { api.endCircle(circleId) }

    override suspend fun cancelCircle(circleId: String): Result<Unit> =
        runCatching { api.cancelCircle(circleId) }

    override suspend fun getToken(circleId: String): Result<CircleToken> =
        runCatching { api.getToken(circleId).toDomain() }

    override fun observeCircleEvents(circleId: String): Flow<CircleRosterEvent> =
        stompClient.subscribe(MeetingWsDestinations.circle(circleId))
            .onStart { stompClient.connect() }
            .mapNotNull { frame ->
                val envelope = decodeEnvelope(frame.body) ?: return@mapNotNull null
                when (envelope.eventType) {
                    "MEMBER_JOINED" -> decodePayload(envelope.payload, CircleMemberDto.serializer())
                        ?.let { CircleRosterEvent.MemberJoined(it.toDomain()) }
                    "MEMBER_LEFT" -> decodeId(envelope.payload)?.let { CircleRosterEvent.MemberLeft(it) }
                    "MEMBER_REMOVED" -> decodeId(envelope.payload)?.let { CircleRosterEvent.MemberRemoved(it) }
                    "CIRCLE_STARTED" -> CircleRosterEvent.Started
                    "CIRCLE_ENDED" -> CircleRosterEvent.Ended
                    "CIRCLE_CANCELLED" -> CircleRosterEvent.Cancelled
                    else -> null
                }
            }

    override fun observeJoinRequestEvents(membershipId: String): Flow<JoinRequestEvent> =
        stompClient.subscribe(MeetingWsDestinations.circleMembership(membershipId))
            .onStart { stompClient.connect() }
            .mapNotNull { frame ->
                val envelope = decodeEnvelope(frame.body) ?: return@mapNotNull null
                when (envelope.eventType) {
                    "REQUEST_APPROVED" -> decodePayload(envelope.payload, CircleMemberDto.serializer())
                        ?.let { JoinRequestEvent.Approved(it.toDomain()) }
                    "REQUEST_REJECTED" -> JoinRequestEvent.Rejected(
                        (envelope.payload as? JsonPrimitive)?.contentOrNull
                            ?: decodePayload(envelope.payload, RejectReasonDto.serializer())
                                ?.let { it.reason ?: it.message },
                    )
                    else -> null
                }
            }

    override fun observePendingRequests(circleId: String): Flow<PendingJoinRequestEvent> =
        stompClient.subscribe(MeetingWsDestinations.circleRequests(circleId))
            .onStart { stompClient.connect() }
            .mapNotNull { frame ->
                val envelope = decodeEnvelope(frame.body) ?: return@mapNotNull null
                when (envelope.eventType) {
                    "CIRCLE_JOIN_REQUEST_RECEIVED" -> decodePayload(envelope.payload, PendingJoinRequestDto.serializer())
                        ?.let { PendingJoinRequestEvent.Received(it.toDomain()) }
                    "CIRCLE_JOIN_REQUEST_REMOVED" -> decodeId(envelope.payload)
                        ?.let { PendingJoinRequestEvent.Removed(it) }
                    else -> null
                }
            }
}

private fun decodeEnvelope(body: String): SocketEventEnvelope? =
    runCatching { MeetingKitJson.decodeFromString(SocketEventEnvelope.serializer(), body) }.getOrNull()

private fun <T> decodePayload(payload: JsonElement, serializer: kotlinx.serialization.KSerializer<T>): T? =
    runCatching { MeetingKitJson.decodeFromJsonElement(serializer, payload) }.getOrNull()

private fun decodeId(payload: JsonElement): String? =
    (payload as? JsonPrimitive)?.contentOrNull
        ?: runCatching { MeetingKitJson.decodeFromJsonElement<String>(payload) }.getOrNull()

private fun CircleDto.toDomain(): Circle = Circle(
    id = id,
    name = name,
    startDate = startDate,
    endDate = endDate,
    type = runCatching { CircleType.valueOf(type) }.getOrDefault(CircleType.PUBLIC),
    status = runCatching { CircleStatus.valueOf(status) }.getOrDefault(CircleStatus.SCHEDULED),
    requiresApproval = requiresApproval,
    maxParticipants = maxParticipants,
    currentMembers = currentMembers,
    host = host?.toDomain(),
)

private fun CircleMemberDto.toDomain(): CircleMember = CircleMember(
    id = id,
    userId = userId,
    displayName = displayName.ifBlank { userId },
    initials = initials.ifBlank { displayName.firstOrNull()?.toString() ?: "" },
    avatarUrl = avatarUrl,
    role = runCatching { CircleMemberRole.valueOf(role) }.getOrDefault(CircleMemberRole.MEMBER),
)

private fun PendingJoinRequestDto.toDomain(): PendingJoinRequest = PendingJoinRequest(
    membershipId = membershipId,
    userId = userId,
    displayName = displayName.ifBlank { userId },
    initials = initials.ifBlank { displayName.firstOrNull()?.toString() ?: "" },
    avatarUrl = avatarUrl,
    joinedAt = joinedAt,
)

private fun com.iti.meeting.data.remote.dto.CircleTokenDto.toDomain(): CircleToken = CircleToken(
    token = token,
    channelName = channelName,
    userAccount = userAccount,
)

private fun CreateCircleRequest.toDto(): com.iti.meeting.data.remote.dto.CreateCircleRequestDto =
    com.iti.meeting.data.remote.dto.CreateCircleRequestDto(
        name = name,
        startDate = startDate,
        endDate = endDate,
        type = type.name,
        requiresApproval = requiresApproval,
        maxParticipants = maxParticipants,
        password = password,
    )
