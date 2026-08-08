package com.iti.meeting.data.remote

import android.util.Log
import com.iti.domain.auth.MeetingAuthTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val MeetingKitJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

fun createMeetingHttpClient(
    tokenProvider: MeetingAuthTokenProvider,
    restBaseUrl: String,
    json: Json = MeetingKitJson,
    engine: HttpClientEngine = OkHttp.create(),
    enableLogging: Boolean = false,
): HttpClient = HttpClient(engine) {

    expectSuccess = true

    install(ContentNegotiation) {
        json(json)
    }

    install(WebSockets)

    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        socketTimeoutMillis = SOCKET_TIMEOUT_MS
    }

    if (enableLogging) {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("MeetingHttpClient", message)
                }
            }
            level = LogLevel.BODY
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }


    install(Auth) {
        reAuthorizeOnResponse { response ->
            response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden
        }
        bearer {
            loadTokens {
                tokenProvider.currentToken()?.let { BearerTokens(it, it) }
            }
            refreshTokens {
                var freshToken: String? = null
                var attempt = 0
                while (freshToken == null && attempt < MAX_REFRESH_ATTEMPTS) {
                    freshToken = tokenProvider.refreshToken()
                    attempt++
                }
                if (freshToken != null) {
                    BearerTokens(freshToken, freshToken)
                } else {
                    tokenProvider.onAuthenticationExpired()
                    null
                }
            }
            sendWithoutRequest { true }
        }
    }

    defaultRequest {
        url(restBaseUrl)
        accept(ContentType.Application.Json)
    }
}

private const val REQUEST_TIMEOUT_MS = 30_000L
private const val CONNECT_TIMEOUT_MS = 15_000L
private const val SOCKET_TIMEOUT_MS = 30_000L
private const val MAX_REFRESH_ATTEMPTS = 2
