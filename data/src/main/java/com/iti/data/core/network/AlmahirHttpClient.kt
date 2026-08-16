package com.iti.data.core.network

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
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.RefreshTokensParams
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
    refreshEndpoint: String = AlmahirApi.Auth.REFRESH,
    isPublicEndpoint: (String) -> Boolean = AlmahirApi.Auth::isPublic,
): HttpClient {
    val log = httpLog(HttpLogSource.ALMAHIR, enableLogging)

    val client = HttpClient(engine) {

        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }

        installHttpLogging(log)

        install(createPublicEndpointGuard(isPublicEndpoint))

        install(Auth) {
            reAuthorizeOnResponse { response ->
                response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden
            }
            bearer {
                loadTokens {
                    tokenStore.getTokens()?.toBearerTokens().also { tokens ->
                        log?.log("AUTH loaded stored session: access=${tokens?.accessToken.tokenFingerprint()}")
                    }
                }
                refreshTokens { refreshSession(tokenStore, refreshEndpoint, log) }
                sendWithoutRequest { request -> !request.isPublicEndpoint(isPublicEndpoint) }
            }
        }

        defaultRequest {
            url(AlmahirApi.BASE_URL)
            accept(ContentType.Application.Json)
        }
    }

    client.dropCachedTokenOnSessionChange(tokenStore, log)
    return client
}


private fun HttpClient.dropCachedTokenOnSessionChange(tokenStore: TokenStore, log: HttpLog?) {
    tokenStore.tokens
        .map { it?.accessToken }
        .distinctUntilChanged()
        .drop(1)
        .onEach {
            log?.log("AUTH stored session changed — dropping the cached bearer token")
            authProvider<BearerAuthProvider>()?.clearToken()
        }
        .launchIn(this)
}


private fun createPublicEndpointGuard(isPublicEndpoint: (String) -> Boolean) =
    createClientPlugin("PublicEndpointGuard") {
        onRequest { request, _ ->
            if (request.isPublicEndpoint(isPublicEndpoint)) request.attributes.put(AuthCircuitBreaker, Unit)
        }
    }

private fun HttpRequestBuilder.isPublicEndpoint(isPublicEndpoint: (String) -> Boolean): Boolean =
    isPublicEndpoint(url.encodedPathSegments.joinToString("/"))


private suspend fun RefreshTokensParams.refreshSession(
    tokenStore: TokenStore,
    refreshEndpoint: String,
    log: HttpLog?,
): BearerTokens? {
    log?.log(
        "AUTH ${response.status.value} on ${response.request.method.value} " +
            "/${response.request.url.encodedPath.trimStart('/')} — refreshing " +
            "(rejected access=${oldTokens?.accessToken.tokenFingerprint()})"
    )

    val stored = tokenStore.getTokens()
    val rejectedAccessToken = oldTokens?.accessToken
    if (stored != null && rejectedAccessToken != null && stored.accessToken != rejectedAccessToken) {
        log?.log("AUTH another client already refreshed — reusing the stored token")
        return stored.toBearerTokens()
    }

    val refreshToken = stored?.refreshToken
    if (refreshToken == null) {
        log?.log("AUTH no refresh token stored — cannot refresh, request stays failed")
        return null
    }

    val refreshResponse = runCatching {
        client.post(refreshEndpoint) {
            markAsRefreshTokenRequest()
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(refreshToken))
        }
    }.getOrElse { error ->
        log?.log("AUTH refresh call to /$refreshEndpoint failed: ${error::class.simpleName}: ${error.message}")
        return null
    }

    if (!refreshResponse.status.isSuccess()) {
        log?.log("AUTH refresh rejected by /$refreshEndpoint: ${refreshResponse.status}")
        if (refreshResponse.status in SESSION_INVALID_STATUSES) {
            log?.log("AUTH refresh token is no longer valid — clearing session")
            tokenStore.clear()
        }
        return null
    }

    val refreshed = refreshResponse.readTokenPair(fallbackRefreshToken = refreshToken)
    if (refreshed == null) {
        log?.log("AUTH refresh succeeded but carried no access token — keeping session, request stays failed")
        return null
    }
    tokenStore.save(refreshed)
    log?.log(
        "AUTH refreshed OK: access=${refreshed.accessToken.tokenFingerprint()}, " +
            "refresh=${refreshed.refreshToken.tokenFingerprint()} — replaying request"
    )
    return refreshed.toBearerTokens()
}

private suspend fun HttpResponse.readTokenPair(fallbackRefreshToken: String): TokenPair? {
    val envelope = runCatching { body<ApiResponse<TokenPairDto>>() }.getOrNull() ?: return null
    if (!envelope.isSuccessful) return null
    val payload = envelope.data ?: return null
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

private val SESSION_INVALID_STATUSES = setOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden)
