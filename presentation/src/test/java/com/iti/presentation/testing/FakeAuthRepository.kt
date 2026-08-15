package com.iti.presentation.testing

import com.iti.domain.auth.model.AuthData
import com.iti.domain.auth.model.AuthTokens
import com.iti.domain.auth.model.User
import com.iti.domain.auth.repository.AuthRepository
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository(
    private val loginResult: Result<AuthData> = Result.Success(AUTH_DATA),
    private val registerResult: Result<User> = Result.Success(USER),
    private val logoutResult: Result<Unit> = Result.Success(Unit),
) : AuthRepository {

    private val signedIn = MutableStateFlow(true)
    override val authState: Flow<Boolean> = signedIn

    var loggedOut = false
        private set

    val registrations = mutableListOf<String>()
    val logins = mutableListOf<String>()
    val googleTokens = mutableListOf<String>()

    override suspend fun register(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phoneNumber: String,
        gender: String,
    ): Result<User> {
        registrations += email
        return registerResult
    }

    override suspend fun login(email: String, password: String): Result<AuthData> {
        logins += email
        return loginResult
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthData> {
        googleTokens += idToken
        return loginResult
    }

    override suspend fun logout(): Result<Unit> {
        loggedOut = logoutResult is Result.Success
        signedIn.value = !loggedOut
        return logoutResult
    }

    override suspend fun getProfile(): Result<User> = Result.Success(USER)

    override suspend fun refreshTokens(): Result<AuthTokens> = Result.Success(TOKENS)

    override suspend fun forgotPassword(email: String): Result<Unit> = Result.Success(Unit)

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun verifyOtp(email: String, otp: String): Result<Unit> = Result.Success(Unit)

    companion object {
        val TOKENS = AuthTokens(accessToken = "access", refreshToken = "refresh")

        val USER = User(
            id = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            username = "mahir",
            firstName = "Yasser",
            lastName = "Ali",
            email = "yasser@example.com",
        )

        val AUTH_DATA = AuthData(tokens = TOKENS, user = USER)

        fun failure(message: String = "boom") = Result.Error(DomainError.ServerError(message))
    }
}
