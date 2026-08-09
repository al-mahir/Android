package com.iti.data.core.network

import android.util.Log
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders

const val HTTP_LOG_TAG = "AlmahirHttp"

object HttpLogSource {
    const val ALMAHIR = "almahir"
    const val MEETING = "meeting"
    const val REFRESH = "refresh"
}

fun interface HttpLog {
    fun log(message: String)
}

fun httpLog(source: String, enabled: Boolean): HttpLog? =
    if (enabled) HttpLog { message -> Log.d(HTTP_LOG_TAG, "[$source] ${message.redactTokenQuery()}") } else null


private fun String.redactTokenQuery(): String =
    TOKEN_QUERY.replace(this) { match -> "${match.groupValues[1]}=${match.groupValues[2].tokenFingerprint()}" }

private val TOKEN_QUERY = Regex("""\b(token|access_token|accessToken)=([^&\s]+)""", RegexOption.IGNORE_CASE)

fun HttpClientConfig<*>.installHttpLogging(log: HttpLog?) {
    if (log == null) return
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) = log.log(message)
        }
        level = LogLevel.BODY
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
}


fun String?.tokenFingerprint(): String =
    if (isNullOrBlank()) "none" else "…${takeLast(FINGERPRINT_CHARS)}(len=$length)"

private const val FINGERPRINT_CHARS = 6
