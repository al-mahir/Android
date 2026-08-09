package com.iti.meeting.data.remote

import com.iti.data.core.network.HttpLog
import com.iti.data.core.network.HttpLogSource
import com.iti.data.core.network.httpLog
import com.iti.data.core.network.installHttpLogging
import com.iti.data.core.network.tokenFingerprint
import com.iti.domain.auth.MeetingAuthTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.accept
import io.ktor.client.request.url
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
): HttpClient {
    val log = httpLog(HttpLogSource.MEETING, enableLogging)

    val client = HttpClient(engine) {

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

        installHttpLogging(log)

        install(Auth) {
            reAuthorizeOnResponse { response ->
                response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden
            }
            bearer {
                loadTokens {
                    tokenProvider.currentToken()?.let { BearerTokens(it, it) }.also { tokens ->
                        log?.log("AUTH loaded host session: access=${tokens?.accessToken.tokenFingerprint()}")
                    }
                }
                refreshTokens {
                    // The refresh itself happens in the host app (:data's TokenRefresher) against
                    // the student- or sheikh-specific endpoint, so it is logged there under the
                    // same tag — these lines bracket it so the handoff is visible from this side.
                    log?.log(
                        "AUTH ${response.status.value} on ${response.request.method.value} " +
                            "/${response.request.url.encodedPath.trimStart('/')} — asking host to refresh " +
                            "(rejected access=${oldTokens?.accessToken.tokenFingerprint()})"
                    )
                    val rejectedToken = oldTokens?.accessToken
                    var freshToken: String? = null
                    var attempt = 0
                    while (freshToken == null && attempt < MAX_REFRESH_ATTEMPTS) {
                        attempt++
                        freshToken = tokenProvider.refreshToken(rejectedToken)
                        if (freshToken == null) log?.log("AUTH host refresh attempt $attempt failed")
                    }
                    if (freshToken != null) {
                        log?.log(
                            "AUTH host returned a fresh token on attempt $attempt: " +
                                "access=${freshToken.tokenFingerprint()} — replaying request"
                        )
                        BearerTokens(freshToken, freshToken)
                    } else {
                        log?.log("AUTH host could not refresh after $attempt attempt(s) — ending session")
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

    client.dropCachedTokenOnSessionChange(tokenProvider, log)
    return client
}

private fun HttpClient.dropCachedTokenOnSessionChange(
    tokenProvider: MeetingAuthTokenProvider,
    log: HttpLog?,
) {
    tokenProvider.sessionChanges
        .onEach {
            log?.log("AUTH host session changed — dropping the cached bearer token")
            authProvider<BearerAuthProvider>()?.clearToken()
        }
        .launchIn(this)
}

private const val REQUEST_TIMEOUT_MS = 30_000L
private const val CONNECT_TIMEOUT_MS = 15_000L
private const val SOCKET_TIMEOUT_MS = 30_000L
private const val MAX_REFRESH_ATTEMPTS = 2
