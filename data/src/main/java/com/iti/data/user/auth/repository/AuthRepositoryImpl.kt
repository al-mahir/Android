package com.iti.data.user.auth.repository

import com.iti.data.user.auth.remote.AuthRemoteDataSource
import com.iti.data.user.auth.remote.dto.AuthDataDto
import com.iti.data.user.auth.remote.dto.ChangePasswordRequest
import com.iti.data.user.auth.remote.dto.GoogleAuthRequest
import com.iti.data.user.auth.remote.dto.LoginRequest
import com.iti.data.user.auth.remote.dto.LogoutRequest
import com.iti.data.user.auth.remote.dto.RegisterRequest
import com.iti.data.user.auth.remote.dto.UserDto
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.core.network.dto.RefreshTokenRequest
import com.iti.data.core.error.toDomainError
import com.iti.data.core.token.TokenPair
import com.iti.data.core.token.TokenStore
import com.iti.data.core.token.getRefreshToken
import com.iti.data.core.token.isLoggedIn
import com.iti.domain.auth.model.AuthData
import com.iti.domain.auth.model.AuthTokens
import com.iti.domain.auth.model.User
import com.iti.domain.auth.repository.AuthRepository
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

import com.iti.data.settings.local.AppPreferencesDataStore

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
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
                )
            )
        },
        onSuccess = { dto ->
            dto.toDomain()
        },
    )

    override suspend fun login(email: String, password: String): Result<AuthData> = apiCall(
        request = { remoteDataSource.login(LoginRequest(email, password)) },
        onSuccess = { it.persistThenMap(fallbackEmail = email) },
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

    override suspend fun forgotPassword(email: String): Result<Unit> =
        apiCallForUnit { remoteDataSource.verifyEmail(email) }

    override suspend fun verifyOtp(email: String, otp: String): Result<Unit> =
        apiCallForUnit { remoteDataSource.verifyOtp(otp, email) }

    override suspend fun resetPassword(email: String, newPassword: String): Result<Unit> =
        apiCallForUnit {
            remoteDataSource.changePassword(
                email = email,
                request = ChangePasswordRequest(password = newPassword, confirmPassword = newPassword)
            )
        }

    override suspend fun getProfile(): Result<User> =
        Result.Error(DomainError.ServerError(NOT_IMPLEMENTED))

    private suspend fun AuthDataDto.persistThenMap(fallbackEmail: String? = null): AuthData {
        val effectiveAccessToken = accessToken ?: token.orEmpty()
        val effectiveRefreshToken = refreshToken.orEmpty()
        val tokens = AuthTokens(effectiveAccessToken, effectiveRefreshToken)
        if (tokens.accessToken.isNotBlank()) {
            tokenStore.save(TokenPair(tokens.accessToken, tokens.refreshToken))
        }

        val domainUser = resolveDomainUser(fallbackEmail)
        if (domainUser.id.isNotBlank()) {
            tokenStore.saveUserId(domainUser.id)
        }

        val appUser = resolveAppPreferencesUser(fallbackEmail)
        // Clear previous user data before saving new user data to prevent stale data leaks
        appPreferencesDataStore.clearUser()
        appPreferencesDataStore.saveUser(appUser)

        return AuthData(
            tokens = tokens,
            user = domainUser,
            isNewUser = isNewUser ?: false,
        )
    }

    private fun AuthDataDto.resolveDomainUser(fallbackEmail: String? = null): User {
        val nested = this.user
        val resolvedId = nested?.id?.ifBlank { null }
            ?: nested?.mongoId?.ifBlank { null }
            ?: this.id?.ifBlank { null }
            ?: this.mongoId?.ifBlank { null }
            ?: ""

        val resolvedUsername = nested?.username
            ?: this.username
            ?: ""

        val resolvedFirstName = nested?.firstName
            ?: this.firstName
            ?: ""

        val resolvedLastName = nested?.lastName
            ?: this.lastName
            ?: ""

        val resolvedEmail = nested?.email
            ?: this.email
            ?: fallbackEmail
            ?: ""

        val resolvedPhone = nested?.phoneNumber
            ?: nested?.snakePhoneNumber
            ?: this.phoneNumber

        val resolvedPic = nested?.profilePictureUrl
            ?: nested?.avatarUrl
            ?: nested?.snakeAvatarUrl
            ?: this.profilePictureUrl
            ?: this.avatarUrl

        val resolvedRoles = if (nested != null && nested.roles.isNotEmpty()) {
            nested.roles
        } else {
            this.roles
        }

        return User(
            id = resolvedId,
            username = resolvedUsername,
            firstName = resolvedFirstName,
            lastName = resolvedLastName,
            email = resolvedEmail,
            phoneNumber = resolvedPhone,
            profilePictureUrl = resolvedPic,
            provider = nested?.provider,
            roles = resolvedRoles,
        )
    }

    private fun AuthDataDto.resolveAppPreferencesUser(fallbackEmail: String? = null): com.iti.domain.model.User {
        val domainUser = resolveDomainUser(fallbackEmail)
        val nested = this.user

        val rawDisplayName = nested?.displayName
            ?: nested?.snakeDisplayName
            ?: nested?.name
            ?: this.displayName
            ?: this.snakeDisplayName
            ?: this.name

        val displayName = when {
            !rawDisplayName.isNullOrBlank() -> rawDisplayName.trim()
            domainUser.firstName.isNotBlank() || domainUser.lastName.isNotBlank() ->
                "${domainUser.firstName} ${domainUser.lastName}".trim()
            domainUser.username.isNotBlank() -> domainUser.username
            domainUser.email.isNotBlank() -> domainUser.email.substringBefore('@')
            !fallbackEmail.isNullOrBlank() -> fallbackEmail.substringBefore('@')
            else -> ""
        }

        val initials = when {
            domainUser.firstName.isNotBlank() && domainUser.lastName.isNotBlank() ->
                "${domainUser.firstName.first()}${domainUser.lastName.first()}".uppercase()
            displayName.isNotBlank() -> {
                val parts = displayName.split(" ").filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    "${parts[0].first()}${parts[1].first()}".uppercase()
                } else {
                    displayName.take(2).uppercase()
                }
            }
            else -> ""
        }

        return com.iti.domain.model.User(
            id = domainUser.id,
            displayName = displayName,
            initials = initials,
            avatarUrl = domainUser.profilePictureUrl,
            email = domainUser.email,
            joinedAtEpochMillis = System.currentTimeMillis(),
        )
    }

    private suspend fun <T, R> apiCall(
        request: suspend () -> ApiResponse<T>,
        onSuccess: suspend (T) -> R,
    ): Result<R> = try {
        val response = request()
        val payload = response.data
        when {
            !response.isSuccessful -> Result.Error(response.toDomainError())
            payload == null -> Result.Error(DomainError.ServerError(response.message ?: EMPTY_PAYLOAD))
            else -> Result.Success(onSuccess(payload))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.Error(throwable.toDomainError())
    }

    private suspend fun apiCallForUnit(
        request: suspend () -> ApiResponse<Unit>,
    ): Result<Unit> = try {
        val response = request()
        if (response.isSuccessful) Result.Success(Unit) else Result.Error(response.toDomainError())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.Error(throwable.toDomainError())
    }

    private fun UserDto?.toDomain(): User = User(
        id = this?.id?.ifBlank { null } ?: this?.mongoId.orEmpty(),
        username = this?.username.orEmpty(),
        firstName = this?.firstName.orEmpty(),
        lastName = this?.lastName.orEmpty(),
        email = this?.email.orEmpty(),
        phoneNumber = this?.phoneNumber ?: this?.snakePhoneNumber,
        profilePictureUrl = this?.profilePictureUrl ?: this?.avatarUrl ?: this?.snakeAvatarUrl,
        provider = this?.provider,
        roles = this?.roles.orEmpty(),
    )

    private companion object {
        const val EMPTY_PAYLOAD = "The server returned an empty response."
        const val NO_ACTIVE_SESSION = "No active session."
        const val NOT_IMPLEMENTED = "Profile endpoint is not available yet."
    }
}
