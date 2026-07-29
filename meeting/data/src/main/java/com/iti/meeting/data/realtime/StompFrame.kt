package com.iti.meeting.data.realtime

data class StompFrame(
    val command: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
) {
    companion object {
        const val COMMAND_CONNECT = "CONNECT"
        const val COMMAND_CONNECTED = "CONNECTED"
        const val COMMAND_SUBSCRIBE = "SUBSCRIBE"
        const val COMMAND_UNSUBSCRIBE = "UNSUBSCRIBE"
        const val COMMAND_SEND = "SEND"
        const val COMMAND_MESSAGE = "MESSAGE"
        const val COMMAND_DISCONNECT = "DISCONNECT"
        const val COMMAND_ERROR = "ERROR"
        const val COMMAND_RECEIPT = "RECEIPT"

        const val HEADER_DESTINATION = "destination"
        const val HEADER_ID = "id"
        const val HEADER_SUBSCRIPTION = "subscription"
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_HEART_BEAT = "heart-beat"
        const val HEADER_ACCEPT_VERSION = "accept-version"
        const val HEADER_CONTENT_LENGTH = "content-length"
    }
}

sealed interface StompIncoming {
    data class Frame(val frame: StompFrame) : StompIncoming
    data object Heartbeat : StompIncoming
}



