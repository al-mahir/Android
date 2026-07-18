package com.iti.data.auth.remote

import com.iti.data.auth.remote.dto.ApiResponse
import com.iti.data.auth.remote.dto.AuthDataDto
import com.iti.data.auth.remote.dto.ForgotPasswordRequest
import com.iti.data.auth.remote.dto.GoogleAuthRequest
import com.iti.data.auth.remote.dto.LoginRequest
import com.iti.data.auth.remote.dto.RegisterRequest
import com.iti.data.auth.remote.dto.ResetPasswordRequest
import com.iti.data.auth.remote.dto.UserDto

// Dummy implementation since Ktor client is not fully set up in the provided structure
// and we are to wire with mocks for missing endpoints as per user approval.
class AuthRemoteDataSource {

    suspend fun register(request: RegisterRequest): ApiResponse<UserDto> {
        // Mock
        return ApiResponse(
            success = true,
            message = "Registered successfully",
            data = UserDto(1, request.username, request.firstName, request.lastName, request.email, request.phoneNumber)
        )
    }

    suspend fun login(request: LoginRequest): ApiResponse<AuthDataDto> {
        // Mock
        return ApiResponse(
            success = true,
            message = "Login successful",
            data = AuthDataDto(
                accessToken = "mock_access_token",
                refreshToken = "mock_refresh_token",
                isNewUser = false,
                user = UserDto(1, "mockuser", "Mock", "User", request.email)
            )
        )
    }

    suspend fun loginWithGoogle(request: GoogleAuthRequest): ApiResponse<AuthDataDto> {
        return ApiResponse(
            success = true,
            message = "Google login successful",
            data = AuthDataDto(
                accessToken = "mock_access_token",
                refreshToken = "mock_refresh_token",
                isNewUser = true,
                user = UserDto(2, "googleuser", "Google", "User", "google@example.com")
            )
        )
    }

    suspend fun forgotPassword(request: ForgotPasswordRequest): ApiResponse<Unit> {
        return ApiResponse(success = true, message = "Reset link sent")
    }

    suspend fun resetPassword(request: ResetPasswordRequest): ApiResponse<Unit> {
        return ApiResponse(success = true, message = "Password reset successfully")
    }

    // Mocked for the UI flow as requested
    suspend fun verifyOtp(email: String, otp: String): ApiResponse<Unit> {
        return ApiResponse(success = true, message = "OTP Verified successfully")
    }

    suspend fun logout(): ApiResponse<Unit> {
         return ApiResponse(success = true, message = "Logged out successfully")
    }
}
