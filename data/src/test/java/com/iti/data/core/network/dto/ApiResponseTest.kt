package com.iti.data.core.network.dto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResponseTest {

    @Test
    fun `treats explicit success true as successful`() {
        val response = ApiResponse(success = true, data = "token")
        assertTrue(response.isSuccessful)
    }

    @Test
    fun `treats explicit success false as failure even with data`() {
        val response = ApiResponse(success = false, data = "token")
        assertFalse(response.isSuccessful)
    }

    @Test
    fun `treats status success as successful`() {
        val response = ApiResponse(status = "success", data = TokenStub("access"))
        assertTrue(response.isSuccessful)
    }

    @Test
    fun `treats 2xx statusCode as successful`() {
        val response = ApiResponse(statusCode = 200, data = TokenStub("access"))
        assertTrue(response.isSuccessful)
    }

    @Test
    fun `treats bare data payload without success flag as successful`() {
        val response = ApiResponse(data = TokenStub("access", "refresh"))
        assertTrue(response.isSuccessful)
    }

    @Test
    fun `treats empty envelope without data as failure`() {
        val response = ApiResponse<String>(message = "Unauthorized")
        assertFalse(response.isSuccessful)
    }

    private data class TokenStub(
        val accessToken: String,
        val refreshToken: String? = null,
    )
}
