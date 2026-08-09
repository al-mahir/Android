package com.iti.data.core.token

import com.iti.data.core.network.AlmahirApi
import com.iti.data.core.network.AlmahirJson
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.core.network.dto.RefreshTokenRequest
import com.iti.data.core.network.dto.TokenPairDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Standalone "refresh the access token right now" capability, on a bare [HttpClient] with no
 * `Auth` plugin of its own — so there's no risk of recursively triggering auth handling on the
 * refresh call itself. Used to back [com.iti.domain.auth.MeetingAuthTokenProvider.refreshToken]
 * (`:meeting:data`'s own HTTP client deliberately has no refresh logic of its own, see that
 * provider's KDoc) since each host app must point it at its own refresh endpoint — the student
 * and sheikh account families are on different backend routes, see [AlmahirApi.Auth].
 *
 * A [Mutex] collapses concurrent callers into a single in-flight refresh, matching the same
 * "one refresh at a time" guarantee Ktor's own `Auth` plugin gives [createAlmahirHttpClient]'s
 * client internally.
 */
class TokenRefresher(
    private val tokenStore: TokenStore,
    private val refreshEndpoint: String,
    engine: HttpClientEngine = Android.create(),
) {
    private val client = HttpClient(engine) {
        install(ContentNegotiation) { json(AlmahirJson) }
        defaultRequest { url(AlmahirApi.BASE_URL) }
    }
    private val mutex = Mutex()

    suspend fun refresh(): TokenPair? = mutex.withLock {
        val refreshToken = tokenStore.getRefreshToken() ?: return@withLock null

        val response = runCatching {
            client.post(refreshEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken))
            }
        }.getOrNull() ?: return@withLock null

        if (!response.status.isSuccess()) {
            if (response.status.value in CLIENT_ERROR_RANGE) tokenStore.clear()
            return@withLock null
        }

        val payload = runCatching { response.body<ApiResponse<TokenPairDto>>() }.getOrNull()?.data ?: return@withLock null
        val accessToken = payload.accessToken?.takeIf { it.isNotBlank() } ?: return@withLock null
        val newTokens = TokenPair(
            accessToken = accessToken,
            refreshToken = payload.refreshToken?.takeIf { it.isNotBlank() } ?: refreshToken,
        )
        tokenStore.save(newTokens)
        newTokens
    }

    private companion object {
        val CLIENT_ERROR_RANGE = 400..499
    }
}
