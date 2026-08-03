package com.iti.meeting.data.remote

import com.iti.data.network.dto.ApiEnvelope
import com.iti.meeting.data.remote.MeetingEndpoints.Circles
import com.iti.meeting.data.remote.dto.CircleDto
import com.iti.meeting.data.remote.dto.CircleMemberDto
import com.iti.meeting.data.remote.dto.CircleTokenDto
import com.iti.meeting.data.remote.dto.CreateCircleRequestDto
import com.iti.meeting.data.remote.dto.JoinCircleRequestDto
import com.iti.meeting.data.remote.dto.JoinCircleResponseDto
import com.iti.meeting.data.remote.dto.PendingJoinRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject

class CircleApi(private val httpClient: HttpClient) {

    suspend fun getPublicCircles(status: String? = null): List<CircleDto> =
        httpClient.get(Circles.BASE) {
            if (status != null) parameter("status", status)
        }.decodeList(CircleDto.serializer())

    suspend fun getMyCircles(): List<CircleDto> =
        httpClient.get(Circles.MINE).decodeList(CircleDto.serializer())

    suspend fun getCircle(circleId: String): CircleDto =
        httpClient.get(Circles.byId(circleId)).decodeBody(CircleDto.serializer())

    suspend fun createCircle(request: CreateCircleRequestDto): CircleDto =
        httpClient.post(Circles.BASE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.decodeBody(CircleDto.serializer())

    suspend fun joinCircle(circleId: String, password: String?): JoinCircleResponseDto =
        httpClient.post(Circles.join(circleId)) {
            contentType(ContentType.Application.Json)
            setBody(JoinCircleRequestDto(password))
        }.decodeBody(JoinCircleResponseDto.serializer())

    suspend fun startCircle(circleId: String): CircleDto =
        httpClient.post(Circles.start(circleId)).decodeBody(CircleDto.serializer())

    suspend fun endCircle(circleId: String) {
        httpClient.post(Circles.end(circleId))
    }

    suspend fun cancelCircle(circleId: String) {
        httpClient.delete(Circles.byId(circleId))
    }

    suspend fun approveJoinRequest(circleId: String, userId: String) {
        httpClient.post(Circles.approve(circleId, userId))
    }

    suspend fun rejectJoinRequest(circleId: String, userId: String) {
        httpClient.post(Circles.reject(circleId, userId))
    }

    suspend fun leaveCircle(circleId: String) {
        httpClient.post(Circles.leave(circleId))
    }

    suspend fun removeMember(circleId: String, userId: String) {
        httpClient.delete(Circles.removeMember(circleId, userId))
    }

    suspend fun getPendingRequests(circleId: String): List<PendingJoinRequestDto> =
        httpClient.get(Circles.pendingRequests(circleId)).decodeList(PendingJoinRequestDto.serializer())

    suspend fun getMembers(circleId: String): List<CircleMemberDto> =
        httpClient.get(Circles.members(circleId)).decodeList(CircleMemberDto.serializer())

    suspend fun getToken(circleId: String): CircleTokenDto =
        httpClient.get(Circles.token(circleId)).decodeBody(CircleTokenDto.serializer())
}

/** Decodes `T` from either the `{success,data}` envelope (the pre-existing API convention) or the
 * raw object — the circle contract documents both styles across revisions. */
private suspend fun <T> HttpResponse.decodeBody(serializer: KSerializer<T>): T {
    val text = bodyAsText()
    runCatching { MeetingKitJson.decodeFromString(ApiEnvelope.serializer(serializer), text) }
        .getOrNull()?.data?.let { return it }
    return MeetingKitJson.decodeFromString(serializer, text)
}

/** Decodes a list from a raw array, an envelope, or an object wrapping a single array field
 * (e.g. `{"content": [...]}`, `{"circles": [...]}`). */
private suspend fun <T> HttpResponse.decodeList(serializer: KSerializer<T>): List<T> {
    val text = bodyAsText()
    val listSerializer = ListSerializer(serializer)
    runCatching { MeetingKitJson.decodeFromString(ApiEnvelope.serializer(listSerializer), text) }
        .getOrNull()?.data?.let { return it }
    runCatching { MeetingKitJson.decodeFromString(listSerializer, text) }.getOrNull()?.let { return it }
    val json = runCatching { MeetingKitJson.parseToJsonElement(text).jsonObject }.getOrNull()
    val arrayElement = json?.values?.firstOrNull { it is JsonArray } ?: return emptyList()
    return runCatching { MeetingKitJson.decodeFromJsonElement(listSerializer, arrayElement) }
        .getOrDefault(emptyList())
}
