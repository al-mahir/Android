package com.iti.data.auth.repository

import com.iti.data.auth.local.TokenStorage
import com.iti.data.auth.remote.AuthRemoteDataSource
import com.iti.data.auth.remote.dto.ForgotPasswordRequest
import com.iti.data.auth.remote.dto.GoogleAuthRequest
import com.iti.data.auth.remote.dto.LoginRequest
import com.iti.data.auth.remote.dto.RegisterRequest
import com.iti.data.auth.remote.dto.ResetPasswordRequest
import com.iti.data.auth.remote.dto.UserDto
import com.iti.domain.auth.model.AuthData
import com.iti.domain.auth.model.AuthTokens
import com.iti.domain.auth.model.User
import com.iti.domain.auth.repository.AuthRepository
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    private val _authState = MutableStateFlow(tokenStorage.getAccessToken() != null)
    override val authState: Flow<Boolean> = _authState.asStateFlow()

    override suspend fun register(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phoneNumber: String
    ): Result<User> {
        return try {
            val response = remoteDataSource.register(
                RegisterRequest(username, firstName, lastName, email, password, password, phoneNumber)
            )
            if (response.success && response.data != null) {
                Result.Success(response.data.toDomain())
            } else {
                Result.Error(DomainError.ServerError(response.message))
            }
        } catch (e: Exception) {
            Result.Error(DomainError.NetworkError(e))
        }
    }

    override suspend fun login(email: String, password: String): Result<AuthData> {
        return try {
            val response = remoteDataSource.login(LoginRequest(email, password))
            if (response.success && response.data != null) {
                val data = response.data
                if (data.accessToken != null && data.refreshToken != null) {
                    tokenStorage.saveAccessToken(data.accessToken)
                    tokenStorage.saveRefreshToken(data.refreshToken)
                    _authState.value = true
                }
                
                Result.Success(
                    AuthData(
                        tokens = AuthTokens(data.accessToken ?: "", data.refreshToken ?: ""),
                        user = data.user?.toDomain() ?: User(0, "", "", "", ""),
                        isNewUser = data.isNewUser ?: false
                    )
                )
            } else {
                Result.Error(DomainError.ServerError(response.message))
            }
        } catch (e: Exception) {
            Result.Error(DomainError.NetworkError(e))
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthData> {
        return try {
            val response = remoteDataSource.loginWithGoogle(GoogleAuthRequest(idToken))
            if (response.success && response.data != null) {
                val data = response.data
                if (data.accessToken != null && data.refreshToken != null) {
                    tokenStorage.saveAccessToken(data.accessToken)
                    tokenStorage.saveRefreshToken(data.refreshToken)
                    _authState.value = true
                }
                
                Result.Success(
                    AuthData(
                        tokens = AuthTokens(data.accessToken ?: "", data.refreshToken ?: ""),
                        user = data.user?.toDomain() ?: User(0, "", "", "", ""),
                        isNewUser = data.isNewUser ?: false
                    )
                )
            } else {
                Result.Error(DomainError.ServerError(response.message))
            }
        } catch (e: Exception) {
            Result.Error(DomainError.NetworkError(e))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            remoteDataSource.logout()
            tokenStorage.clearTokens()
            _authState.value = false
            Result.Success(Unit)
        } catch (e: Exception) {
             Result.Error(DomainError.NetworkError(e))
        }
    }

    override suspend fun getProfile(): Result<User> {
         return Result.Error(DomainError.ServerError("Not fully mocked yet"))
    }

    override suspend fun refreshTokens(): Result<AuthTokens> {
         return Result.Error(DomainError.ServerError("Not fully mocked yet"))
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            val response = remoteDataSource.forgotPassword(ForgotPasswordRequest(email))
            if (response.success) {
                Result.Success(Unit)
            } else {
                Result.Error(DomainError.ServerError(response.message))
            }
        } catch (e: Exception) {
            Result.Error(DomainError.NetworkError(e))
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> {
        return try {
            val response = remoteDataSource.resetPassword(ResetPasswordRequest(token, newPassword, newPassword))
            if (response.success) {
                Result.Success(Unit)
            } else {
                Result.Error(DomainError.ServerError(response.message))
            }
        } catch (e: Exception) {
            Result.Error(DomainError.NetworkError(e))
        }
    }

    override suspend fun verifyOtp(email: String, otp: String): Result<Unit> {
        return try {
            val response = remoteDataSource.verifyOtp(email, otp)
            if (response.success) {
                Result.Success(Unit)
            } else {
                Result.Error(DomainError.ServerError(response.message))
            }
        } catch (e: Exception) {
            Result.Error(DomainError.NetworkError(e))
        }
    }

    private fun UserDto.toDomain() = User(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phoneNumber = phoneNumber
    )
}
