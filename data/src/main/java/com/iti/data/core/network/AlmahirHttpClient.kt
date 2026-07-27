package com.iti.data.core.network

import android.util.Log
import com.iti.data.BuildConfig
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.core.network.dto.RefreshTokenRequest
import com.iti.data.core.network.dto.TokenPairDto
import com.iti.data.core.token.TokenPair
import com.iti.data.core.token.TokenStore
import com.iti.data.core.token.getRefreshToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.RefreshTokensParams
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val AlmahirJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}


fun createAlmahirHttpClient(
    tokenStore: TokenStore,
    json: Json = AlmahirJson,
    engine: HttpClientEngine = Android.create(),
    enableLogging: Boolean = BuildConfig.DEBUG,
): HttpClient = HttpClient(engine) {

    expectSuccess = true

    install(ContentNegotiation) {
        json(json)
    }

    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        socketTimeoutMillis = SOCKET_TIMEOUT_MS
    }

    if (enableLogging) {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("HttpClient", message)
                }
            }
            level = LogLevel.BODY
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }

    install(PublicEndpointGuard)

    install(Auth) {
        bearer {
            loadTokens { tokenStore.getTokens()?.toBearerTokens() }
            refreshTokens { refreshSession(tokenStore) }
            sendWithoutRequest { request -> !request.isPublicEndpoint() }
        }
    }

    defaultRequest {
        url(AlmahirApi.BASE_URL)
        accept(ContentType.Application.Json)
    }
}


private val PublicEndpointGuard = createClientPlugin("PublicEndpointGuard") {
    onRequest { request, _ ->
        if (request.isPublicEndpoint()) request.attributes.put(AuthCircuitBreaker, Unit)
    }
}

private fun HttpRequestBuilder.isPublicEndpoint(): Boolean =
    AlmahirApi.Auth.isPublic(url.encodedPathSegments.joinToString("/"))


private suspend fun RefreshTokensParams.refreshSession(tokenStore: TokenStore): BearerTokens? {
    val refreshToken = tokenStore.getRefreshToken() ?: return null

    val refreshResponse = runCatching {
        client.post(AlmahirApi.Auth.REFRESH) {
            markAsRefreshTokenRequest()
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(refreshToken))
        }
    }.getOrNull() ?: return null

    if (!refreshResponse.status.isSuccess()) {
        if (refreshResponse.status.value in CLIENT_ERROR_RANGE) tokenStore.clear()
        return null
    }

    val refreshed = refreshResponse.readTokenPair(fallbackRefreshToken = refreshToken) ?: return null
    tokenStore.save(refreshed)
    return refreshed.toBearerTokens()
}

private suspend fun HttpResponse.readTokenPair(fallbackRefreshToken: String): TokenPair? {
    val payload = runCatching { body<ApiResponse<TokenPairDto>>() }.getOrNull()?.data ?: return null
    val accessToken = payload.accessToken?.takeIf { it.isNotBlank() } ?: return null
    return TokenPair(
        accessToken = accessToken,
        refreshToken = payload.refreshToken?.takeIf { it.isNotBlank() } ?: fallbackRefreshToken,
    )
}

private fun TokenPair.toBearerTokens() = BearerTokens(accessToken, refreshToken)

private const val REQUEST_TIMEOUT_MS = 30_000L
private const val CONNECT_TIMEOUT_MS = 15_000L
private const val SOCKET_TIMEOUT_MS = 30_000L
private val CLIENT_ERROR_RANGE = 400..499
