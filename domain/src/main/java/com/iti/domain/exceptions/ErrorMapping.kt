package com.iti.domain.exceptions

fun Throwable.toAppException(): Throwable {
    val name = this::class.simpleName ?: ""
    val msg = message ?: ""
    val looksOffline = name.contains("UnknownHost", true) ||
        name.contains("ConnectException", true) ||
        name.contains("SocketTimeout", true) ||
        msg.contains("Unable to resolve host", true) ||
        msg.contains("failed to connect", true)
    return if (looksOffline) NoConnectionException() else this
}
