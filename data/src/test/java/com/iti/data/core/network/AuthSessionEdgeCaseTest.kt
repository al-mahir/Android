package com.iti.data.core.network

import com.iti.data.core.token.TokenPair
import com.iti.data.core.token.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins down the session behaviour the two apps depend on but that isn't obvious from reading
 * [createAlmahirHttpClient] alone — it lives in Ktor's `Auth` plugin (which token is cached, when
 * it is re-read, how concurrent 401s collapse). These are the cases a refresh bug actually shows
 * up in, and they are cheap to get wrong when touching the auth wiring.
 */
class AuthSessionEdgeCaseTest {

    private val refreshCalls = AtomicInteger(0)
    private val protectedCalls = mutableListOf<String?>()

    @Test
    fun `sends one refresh for a burst of concurrent 401s and replays them all`() = runBlocking {
        val store = FakeTokenStore(TokenPair(OLD_ACCESS, OLD_REFRESH))
        val client = clientWith(store)

        val responses = (1..CONCURRENT_CALLS)
            .map { async { client.get(PROTECTED_PATH).status } }
            .awaitAll()

        assertEquals(List(CONCURRENT_CALLS) { HttpStatusCode.OK }, responses)
        assertEquals(1, refreshCalls.get())
    }

    @Test
    fun `persists the rotated refresh token so the next refresh uses it`() = runBlocking {
        val store = FakeTokenStore(TokenPair(OLD_ACCESS, OLD_REFRESH))
        val client = clientWith(store)

        client.get(PROTECTED_PATH)

        assertEquals(TokenPair(NEW_ACCESS, NEW_REFRESH), store.getTokens())
    }

    @Test
    fun `re-reads the store instead of reusing the token that just failed to refresh`() = runBlocking {
        // Refresh is refused, so the session is dropped; a later request must not keep replaying
        // the dead access token — it has to notice the store is empty.
        val store = FakeTokenStore(TokenPair(OLD_ACCESS, OLD_REFRESH))
        val client = clientWith(store, refreshStatus = HttpStatusCode.Unauthorized)

        runCatching { client.get(PROTECTED_PATH) }
        protectedCalls.clear()
        runCatching { client.get(PROTECTED_PATH) }

        assertEquals(listOf(null), protectedCalls)
    }

    @Test
    fun `uses the newly stored token after signing out and back in`() = runBlocking {
        // Sign-out clears the store and sign-in writes a different account's tokens. The very next
        // protected call must carry the new access token — sending the previous session's would,
        // if it has not expired server-side yet, answer one account's request with another's data.
        val store = FakeTokenStore(TokenPair(OLD_ACCESS, OLD_REFRESH))
        val client = clientWith(store)
        client.get(PROTECTED_PATH)

        store.clear()
        store.save(TokenPair(OTHER_ACCESS, OTHER_REFRESH))
        awaitSessionChangePropagation()
        protectedCalls.clear()
        runCatching { client.get(PROTECTED_PATH) }

        assertEquals("Bearer $OTHER_ACCESS", protectedCalls.first())
    }

    @Test
    fun `reuses a token another client already refreshed instead of spending the refresh token`() =
        runBlocking {
            // A backend that rotates refresh tokens on use invalidates the old one, so a second
            // client racing to refresh would get a 401 and drop a live session.
            val store = FakeTokenStore(TokenPair(OLD_ACCESS, OLD_REFRESH))
            val client = clientWith(store)
            store.save(TokenPair(NEW_ACCESS, NEW_REFRESH))
            awaitSessionChangePropagation()

            client.get(PROTECTED_PATH)

            assertEquals(0, refreshCalls.get())
        }

    /**
     * The cached token is dropped by a collector running on the client's own scope, so it lands
     * shortly after `save` rather than inline with it. Nothing in the client exposes that the
     * collector has caught up, so give it a moment.
     */
    private suspend fun awaitSessionChangePropagation() = delay(PROPAGATION_MS)

    private fun clientWith(
        store: TokenStore,
        refreshStatus: HttpStatusCode = HttpStatusCode.OK,
    ): HttpClient = createAlmahirHttpClient(
        tokenStore = store,
        enableLogging = false,
        engine = MockEngine { request -> respondTo(request, refreshStatus) },
    )

    private suspend fun io.ktor.client.engine.mock.MockRequestHandleScope.respondTo(
        request: HttpRequestData,
        refreshStatus: HttpStatusCode,
    ) = when {
        request.url.encodedPath.endsWith(AlmahirApi.Auth.REFRESH) -> {
            refreshCalls.incrementAndGet()
            respondJson(REFRESHED_BODY, refreshStatus)
        }

        else -> {
            val authorization = request.headers[HttpHeaders.Authorization]
            protectedCalls += authorization
            if (authorization == "Bearer $NEW_ACCESS" || authorization == "Bearer $OTHER_ACCESS") {
                respondJson(SUCCESS_BODY)
            } else {
                respondJson(UNAUTHORIZED_BODY, HttpStatusCode.Unauthorized)
            }
        }
    }

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    private class FakeTokenStore(initial: TokenPair?) : TokenStore {
        private val state = MutableStateFlow(initial)
        private val userState = MutableStateFlow<String?>(null)
        override val tokens: Flow<TokenPair?> = state
        override val userId: Flow<String?> = userState
        override suspend fun getTokens(): TokenPair? = state.value
        override suspend fun getUserId(): String? = userState.value
        override suspend fun save(tokens: TokenPair) {
            state.value = tokens
        }

        override suspend fun saveUserId(userId: String) {
            userState.value = userId
        }

        override suspend fun clear() {
            state.value = null
            userState.value = null
        }
    }

    private companion object {
        const val PROTECTED_PATH = "api/sheikh"
        const val CONCURRENT_CALLS = 5
        const val PROPAGATION_MS = 200L

        const val OLD_ACCESS = "old-access"
        const val OLD_REFRESH = "old-refresh"
        const val NEW_ACCESS = "new-access"
        const val NEW_REFRESH = "new-refresh"
        const val OTHER_ACCESS = "other-access"
        const val OTHER_REFRESH = "other-refresh"

        const val SUCCESS_BODY = """{"success":true,"message":"ok","data":{}}"""
        const val UNAUTHORIZED_BODY = """{"success":false,"message":"Unauthorized"}"""
        const val REFRESHED_BODY =
            """{"success":true,"message":"ok","data":{"accessToken":"$NEW_ACCESS","refreshToken":"$NEW_REFRESH"}}"""
    }
}
