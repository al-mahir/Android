package com.iti.data.auth.remote

import com.iti.data.auth.remote.dto.ApiResponse
import com.iti.data.auth.remote.dto.AuthDataDto
import com.iti.data.auth.remote.dto.ForgotPasswordRequest
import com.iti.data.auth.remote.dto.GoogleAuthRequest
import com.iti.data.auth.remote.dto.LoginRequest
import com.iti.data.auth.remote.dto.RegisterRequest
import com.iti.data.auth.remote.dto.ResetPasswordRequest
import com.iti.data.auth.remote.dto.UserDto

import com.iti.data.auth.local.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AuthRemoteDataSource(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage
) {

    suspend fun register(request: RegisterRequest): ApiResponse<UserDto> {
        return client.post("auth/register") {
            setBody(request)
        }.body()
    }

    suspend fun login(request: LoginRequest): ApiResponse<AuthDataDto> {
        return client.post("auth/login") {
            setBody(request)
        }.body()
    }

    suspend fun loginWithGoogle(request: GoogleAuthRequest): ApiResponse<AuthDataDto> {
        return client.post("auth/google") {
            setBody(request)
        }.body()
    }

    suspend fun forgotPassword(request: ForgotPasswordRequest): ApiResponse<Unit> {
        return client.post("auth/forgot-password") {
            setBody(request)
        }.body()
    }

    suspend fun resetPassword(request: ResetPasswordRequest): ApiResponse<Unit> {
        return client.post("auth/reset-password") {
            setBody(request)
        }.body()
    }

    // Mocked for the UI flow as requested
    suspend fun verifyOtp(email: String, otp: String): ApiResponse<Unit> {
        return ApiResponse(success = true, message = "OTP Verified successfully")
    }

    suspend fun logout(): ApiResponse<Unit> {
        val token = tokenStorage.getAccessToken()
        return client.post("auth/logout") {
            if (token != null) {
                header("Authorization", "Bearer $token")
            }
        }.body()
    }
}
