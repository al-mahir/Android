package com.iti.data.core.token

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class AccessTokenExpiryTest {

    @Test
    fun `returns false for malformed token`() {
        assertFalse("not-a-jwt".isAccessTokenExpiredOrNearExpiry())
    }

    @Test
    fun `detects expired token`() {
        val exp = (System.currentTimeMillis() / 1_000L) - 120
        val token = jwtWithExp(exp)
        assertTrue(token.isAccessTokenExpiredOrNearExpiry())
    }

    @Test
    fun `detects token within leeway window`() {
        val exp = (System.currentTimeMillis() / 1_000L) + 30
        val token = jwtWithExp(exp)
        assertTrue(token.isAccessTokenExpiredOrNearExpiry())
    }

    @Test
    fun `accepts fresh token outside leeway window`() {
        val exp = (System.currentTimeMillis() / 1_000L) + 600
        val token = jwtWithExp(exp)
        assertFalse(token.isAccessTokenExpiredOrNearExpiry())
    }

    private fun jwtWithExp(expSeconds: Long): String {
        val header = base64Url("""{"alg":"none","typ":"JWT"}""")
        val payload = base64Url("""{"exp":$expSeconds}""")
        return "$header.$payload.signature"
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun base64Url(value: String): String =
        Base64.UrlSafe.encode(value.toByteArray())
}
