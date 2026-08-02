package com.iti.data.core.network

import com.iti.data.core.token.TokenPair
import com.iti.data.core.token.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlmahirHttpClientTest {

    private val recordedRequests = mutableListOf<HttpRequestData>()

    @Test
    fun `attaches the stored access token to protected requests`() = runBlocking {
        val store = FakeTokenStore(TokenPair(ACCESS_TOKEN, REFRESH_TOKEN))
        val client = clientWith(store) { respondJson(SUCCESS_BODY) }

        client.post(AlmahirApi.Auth.LOGOUT) { jsonBody() }

        assertEquals("Bearer $ACCESS_TOKEN", recordedRequests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `refreshes the session and replays the request after a 401`() = runBlocking {
        val store = FakeTokenStore(TokenPair(ACCESS_TOKEN, REFRESH_TOKEN))
        val client = clientWith(store) { request ->
            when {
                request.url.encodedPath.endsWith(AlmahirApi.Auth.REFRESH) -> respondJson(REFRESHED_BODY)
                request.headers[HttpHeaders.Authorization] == "Bearer $NEW_ACCESS_TOKEN" -> respondJson(SUCCESS_BODY)
                else -> respondJson(UNAUTHORIZED_BODY, HttpStatusCode.Unauthorized)
            }
        }

        client.post(AlmahirApi.Auth.LOGOUT) { jsonBody() }

        assertEquals(3, recordedRequests.size)
        assertEquals("Bearer $NEW_ACCESS_TOKEN", recordedRequests.last().headers[HttpHeaders.Authorization])
        assertEquals(TokenPair(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN), store.getTokens())
    }

    @Test
    fun `refreshes the session and replays the request after a 403`() = runBlocking {
        val store = FakeTokenStore(TokenPair(ACCESS_TOKEN, REFRESH_TOKEN))
        val client = clientWith(store) { request ->
            when {
                request.url.encodedPath.endsWith(AlmahirApi.Auth.REFRESH) -> respondJson(REFRESHED_BODY)
                request.headers[HttpHeaders.Authorization] == "Bearer $NEW_ACCESS_TOKEN" -> respondJson(SUCCESS_BODY)
                else -> respondJson(UNAUTHORIZED_BODY, HttpStatusCode.Forbidden)
            }
        }

        client.post(AlmahirApi.Auth.LOGOUT) { jsonBody() }

        assertEquals(3, recordedRequests.size)
        assertEquals("Bearer $NEW_ACCESS_TOKEN", recordedRequests.last().headers[HttpHeaders.Authorization])
        assertEquals(TokenPair(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN), store.getTokens())
    }

    @Test
    fun `does not refresh when a public endpoint rejects the credentials`() = runBlocking {
        val store = FakeTokenStore(TokenPair(ACCESS_TOKEN, REFRESH_TOKEN))
        val client = clientWith(store) { respondJson(UNAUTHORIZED_BODY, HttpStatusCode.Unauthorized) }

        val failure = runCatching { client.post(AlmahirApi.Auth.LOGIN) { jsonBody() } }.exceptionOrNull()

        assertTrue(failure is ClientRequestException)
        assertEquals(1, recordedRequests.size)
        assertNull(recordedRequests.single().headers[HttpHeaders.Authorization])
        assertEquals(TokenPair(ACCESS_TOKEN, REFRESH_TOKEN), store.getTokens())
    }

    @Test
    fun `drops the session when the refresh token is rejected`() = runBlocking {
        val store = FakeTokenStore(TokenPair(ACCESS_TOKEN, REFRESH_TOKEN))
        val client = clientWith(store) { respondJson(UNAUTHORIZED_BODY, HttpStatusCode.Unauthorized) }

        runCatching { client.post(AlmahirApi.Auth.LOGOUT) { jsonBody() } }

        assertNull(store.getTokens())
    }

    private fun clientWith(
        store: TokenStore,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient = createAlmahirHttpClient(
        tokenStore = store,
        engine = MockEngine { request ->
            recordedRequests += request
            handler(request)
        },
        enableLogging = false,
    )

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    private fun HttpRequestBuilder.jsonBody() {
        contentType(ContentType.Application.Json)
        setBody("""{"refreshToken":"$REFRESH_TOKEN"}""")
    }

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
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
        const val NEW_ACCESS_TOKEN = "new-access-token"
        const val NEW_REFRESH_TOKEN = "new-refresh-token"

        const val SUCCESS_BODY = """{"success":true,"message":"ok","data":{}}"""
        const val UNAUTHORIZED_BODY = """{"success":false,"message":"Unauthorized"}"""
        const val REFRESHED_BODY =
            """{"success":true,"message":"ok","data":{"accessToken":"$NEW_ACCESS_TOKEN","refreshToken":"$NEW_REFRESH_TOKEN"}}"""
    }
}
