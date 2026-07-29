package com.iti.meeting.data.realtime

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull


@Serializable
data class SocketEventEnvelope(
    val eventType: String,
    val payload: JsonElement = JsonNull,
)



