package com.iti.data.sheikh.auth.repository

import com.iti.data.core.error.toDomainError
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.core.network.dto.RefreshTokenRequest
import com.iti.data.core.token.TokenPair
import com.iti.data.core.token.TokenStore
import com.iti.data.core.token.getRefreshToken
import com.iti.data.core.token.isLoggedIn
import com.iti.data.settings.local.AppPreferencesDataStore
import com.iti.data.sheikh.auth.remote.SheikhAuthRemoteDataSource
import com.iti.data.user.auth.remote.dto.AuthDataDto
import com.iti.data.user.auth.remote.dto.GoogleAuthRequest
import com.iti.data.user.auth.remote.dto.LoginRequest
import com.iti.data.user.auth.remote.dto.LogoutRequest
import com.iti.data.user.auth.remote.dto.RegisterRequest
import com.iti.data.user.auth.remote.dto.UserDto
import com.iti.domain.auth.model.AuthData
import com.iti.domain.auth.model.AuthTokens
import com.iti.domain.auth.model.User
import com.iti.domain.auth.repository.AuthRepository
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * [AuthRepository] implementation for sheikh accounts — same contract as
 * [com.iti.data.user.auth.repository.AuthRepositoryImpl], talking to the sheikh-namespaced
 * endpoints via [SheikhAuthRemoteDataSource] instead. `:sheikh-app` binds `AuthRepository` to
 * this implementation instead of the student one, so the reused auth screens in `:presentation`
 * work unmodified against either backend.
 */
class SheikhAuthRepositoryImpl(
    private val remoteDataSource: SheikhAuthRemoteDataSource,
    private val tokenStore: TokenStore,
    private val appPreferencesDataStore: AppPreferencesDataStore,
) : AuthRepository {

    override val authState: Flow<Boolean> = tokenStore.isLoggedIn

    override suspend fun register(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phoneNumber: String,
        gender: String,
    ): Result<User> = apiCall(
        request = {
            remoteDataSource.register(
                RegisterRequest(
                    username = username,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    password = password,
                    phoneNumber = phoneNumber,
                    gender = gender,
                )
            )
        },
        onSuccess = { dto ->
            val user = dto.toDomain()
            tokenStore.saveUserId(user.id)
            appPreferencesDataStore.saveUser(user.toAppPreferencesUser())
            user
        },
    )

    override suspend fun login(email: String, password: String): Result<AuthData> = apiCall(
        request = { remoteDataSource.login(LoginRequest(email, password)) },
        onSuccess = { it.persistThenMap() },
    )

    override suspend fun loginWithGoogle(idToken: String): Result<AuthData> = apiCall(
        request = { remoteDataSource.loginWithGoogle(GoogleAuthRequest(idToken)) },
        onSuccess = { it.persistThenMap() },
    )

    override suspend fun refreshTokens(): Result<AuthTokens> {
        val refreshToken = tokenStore.getRefreshToken()
            ?: return Result.Error(DomainError.Unauthorized(NO_ACTIVE_SESSION))

        return apiCall(
            request = { remoteDataSource.refresh(RefreshTokenRequest(refreshToken)) },
            onSuccess = { it.persistThenMap().tokens },
        )
    }

    override suspend fun logout(): Result<Unit> {
        val refreshToken = tokenStore.getRefreshToken()
        return try {
            refreshToken?.let { remoteDataSource.logout(LogoutRequest(it)) }
            Result.Success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            Result.Success(Unit)
        } finally {
            tokenStore.clear()
            appPreferencesDataStore.clearUser()
        }
    }

    // No sheikh forgot/reset-password endpoints exist yet.
    override suspend fun forgotPassword(email: String): Result<Unit> =
        Result.Error(DomainError.ServerError(NOT_IMPLEMENTED))

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> =
        Result.Error(DomainError.ServerError(NOT_IMPLEMENTED))

    override suspend fun getProfile(): Result<User> =
        Result.Error(DomainError.ServerError(NOT_IMPLEMENTED))

    // The backend exposes no OTP endpoint yet; the UI flow is unblocked with a local stub —
    // matches the student AuthRepositoryImpl's stub.
    override suspend fun verifyOtp(email: String, otp: String): Result<Unit> = Result.Success(Unit)

    private suspend fun AuthDataDto.persistThenMap(): AuthData {
        val tokens = AuthTokens(accessToken.orEmpty(), refreshToken.orEmpty())
        if (tokens.accessToken.isNotBlank() && tokens.refreshToken.isNotBlank()) {
            tokenStore.save(TokenPair(tokens.accessToken, tokens.refreshToken))
        }
        val domainUser = user.toDomain()
        tokenStore.saveUserId(domainUser.id)
        appPreferencesDataStore.saveUser(domainUser.toAppPreferencesUser())

        return AuthData(
            tokens = tokens,
            user = domainUser,
            isNewUser = isNewUser ?: false,
        )
    }

    private fun User.toAppPreferencesUser() = com.iti.domain.model.User(
        id = id,
        displayName = "$firstName $lastName".trim(),
        initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase(),
        avatarUrl = profilePictureUrl,
        email = email,
        joinedAtEpochMillis = System.currentTimeMillis(),
    )

    private suspend fun <T, R> apiCall(
        request: suspend () -> ApiResponse<T>,
        onSuccess: suspend (T) -> R,
    ): Result<R> = try {
        val response = request()
        val payload = response.data
        when {
            !response.success -> Result.Error(response.toDomainError())
            payload == null -> Result.Error(DomainError.ServerError(response.message ?: EMPTY_PAYLOAD))
            else -> Result.Success(onSuccess(payload))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.Error(throwable.toDomainError())
    }

    private fun UserDto?.toDomain(): User = User(
        id = this?.id.orEmpty(),
        username = this?.username.orEmpty(),
        firstName = this?.firstName.orEmpty(),
        lastName = this?.lastName.orEmpty(),
        email = this?.email.orEmpty(),
        phoneNumber = this?.phoneNumber,
        gender = this?.gender,
        profilePictureUrl = this?.profilePictureUrl,
        provider = this?.provider,
        roles = this?.roles.orEmpty(),
    )

    private companion object {
        const val EMPTY_PAYLOAD = "The server returned an empty response."
        const val NO_ACTIVE_SESSION = "No active session."
        const val NOT_IMPLEMENTED = "This isn't available for sheikh accounts yet."
    }
}
