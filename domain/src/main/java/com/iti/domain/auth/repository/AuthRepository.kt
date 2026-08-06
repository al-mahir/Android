package com.iti.domain.auth.repository

import com.iti.domain.auth.model.AuthData
import com.iti.domain.auth.model.AuthTokens
import com.iti.domain.auth.model.User
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    
    val authState: Flow<Boolean>
    
    suspend fun register(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phoneNumber: String
    ): Result<User>

    suspend fun login(email: String, password: String): Result<AuthData>
    
    suspend fun loginWithGoogle(idToken: String): Result<AuthData>
    
    suspend fun logout(): Result<Unit>
    
    suspend fun getProfile(): Result<User>
    
    suspend fun refreshTokens(): Result<AuthTokens>
    
    suspend fun forgotPassword(email: String): Result<Unit>
    
    suspend fun resetPassword(email: String, newPassword: String): Result<Unit>
    
    suspend fun verifyOtp(email: String, otp: String): Result<Unit>
}

