package com.iti.data.core.token

import com.iti.data.BuildConfig
import com.iti.data.core.network.AlmahirApi
import com.iti.data.core.network.AlmahirJson
import com.iti.data.core.network.HttpLogSource
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.core.network.dto.RefreshTokenRequest
import com.iti.data.core.network.dto.TokenPairDto
import com.iti.data.core.network.httpLog
import com.iti.data.core.network.installHttpLogging
import com.iti.data.core.network.tokenFingerprint
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class TokenRefresher(
    private val tokenStore: TokenStore,
    private val refreshEndpoint: String,
    engine: HttpClientEngine = Android.create(),
    enableLogging: Boolean = BuildConfig.DEBUG,
) {
    private val log = httpLog(HttpLogSource.REFRESH, enableLogging)

    private val client = HttpClient(engine) {
        install(ContentNegotiation) { json(AlmahirJson) }
        installHttpLogging(log)
        defaultRequest { url(AlmahirApi.BASE_URL) }
    }
    private val mutex = Mutex()

    suspend fun refresh(rejectedAccessToken: String? = null): TokenPair? = mutex.withLock {
        val stored = tokenStore.getTokens()
        if (rejectedAccessToken != null && stored != null && stored.accessToken != rejectedAccessToken) {
            log?.log("AUTH another caller already refreshed — reusing the stored token")
            return@withLock stored
        }

        val refreshToken = stored?.refreshToken
        if (refreshToken == null) {
            log?.log("AUTH no refresh token stored — cannot refresh against /$refreshEndpoint")
            return@withLock null
        }
        log?.log("AUTH refreshing via /$refreshEndpoint (refresh=${refreshToken.tokenFingerprint()})")

        val response = runCatching {
            client.post(refreshEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken))
            }
        }.getOrElse { error ->
            log?.log("AUTH refresh call to /$refreshEndpoint failed: ${error::class.simpleName}: ${error.message}")
            return@withLock null
        }

        if (!response.status.isSuccess()) {
            log?.log("AUTH refresh rejected by /$refreshEndpoint: ${response.status}")
            if (response.status in SESSION_INVALID_STATUSES) {
                log?.log("AUTH refresh token is no longer valid — clearing session")
                tokenStore.clear()
            }
            return@withLock null
        }

        val payload = runCatching { response.body<ApiResponse<TokenPairDto>>() }.getOrNull()?.data
        val accessToken = payload?.accessToken?.takeIf { it.isNotBlank() }
        if (accessToken == null) {
            log?.log("AUTH refresh succeeded but carried no access token — keeping session")
            return@withLock null
        }
        val newTokens = TokenPair(
            accessToken = accessToken,
            refreshToken = payload.refreshToken?.takeIf { it.isNotBlank() } ?: refreshToken,
        )
        tokenStore.save(newTokens)
        log?.log(
            "AUTH refreshed OK: access=${newTokens.accessToken.tokenFingerprint()}, " +
                "refresh=${newTokens.refreshToken.tokenFingerprint()}"
        )
        newTokens
    }

    private companion object {

        val SESSION_INVALID_STATUSES = setOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden)
    }
}
