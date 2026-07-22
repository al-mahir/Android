package com.iti.data.auth.repository

import com.iti.data.auth.remote.AuthRemoteDataSource
import com.iti.data.auth.remote.dto.AuthDataDto
import com.iti.data.auth.remote.dto.ForgotPasswordRequest
import com.iti.data.auth.remote.dto.GoogleAuthRequest
import com.iti.data.auth.remote.dto.LoginRequest
import com.iti.data.auth.remote.dto.LogoutRequest
import com.iti.data.auth.remote.dto.RegisterRequest
import com.iti.data.auth.remote.dto.ResetPasswordRequest
import com.iti.data.auth.remote.dto.UserDto
import com.iti.data.core.network.dto.ApiErrorResponse
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.core.network.dto.RefreshTokenRequest
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
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val tokenStore: TokenStore,
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
        onSuccess = { it.toDomain() },
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
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> =
        apiCallForUnit { remoteDataSource.forgotPassword(ForgotPasswordRequest(email)) }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> =
        apiCallForUnit {
            remoteDataSource.resetPassword(ResetPasswordRequest(token, newPassword, newPassword))
        }

    override suspend fun getProfile(): Result<User> =
        Result.Error(DomainError.ServerError(NOT_IMPLEMENTED))

    // The backend exposes no OTP endpoint yet; the UI flow is unblocked with a local stub.
    override suspend fun verifyOtp(email: String, otp: String): Result<Unit> = Result.Success(Unit)

    private suspend fun AuthDataDto.persistThenMap(): AuthData {
        val tokens = AuthTokens(accessToken.orEmpty(), refreshToken.orEmpty())
        if (tokens.accessToken.isNotBlank() && tokens.refreshToken.isNotBlank()) {
            tokenStore.save(TokenPair(tokens.accessToken, tokens.refreshToken))
        }
        return AuthData(
            tokens = tokens,
            user = user.toDomain(),
            isNewUser = isNewUser ?: false,
        )
    }

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

    private suspend fun apiCallForUnit(
        request: suspend () -> ApiResponse<Unit>,
    ): Result<Unit> = try {
        val response = request()
        if (response.success) Result.Success(Unit) else Result.Error(response.toDomainError())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.Error(throwable.toDomainError())
    }

    private fun ApiResponse<*>.toDomainError(): DomainError {
        val reason = message ?: UNKNOWN_ERROR
        val errors = fieldErrors.orEmpty()
        return if (errors.isEmpty()) {
            DomainError.ServerError(reason)
        } else {
            DomainError.ValidationError(reason, errors)
        }
    }

    private suspend fun Throwable.toDomainError(): DomainError {
        if (this !is ResponseException) return DomainError.NetworkError(this)

        val status = response.status
        val error = runCatching { response.body<ApiErrorResponse>() }.getOrNull()
        val reason = error?.message ?: status.description
        val fieldErrors = error?.fieldErrors.orEmpty()

        return when (status.value) {
            HTTP_BAD_REQUEST, HTTP_UNPROCESSABLE_ENTITY -> DomainError.ValidationError(reason, fieldErrors)
            HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> DomainError.Unauthorized(reason)
            HTTP_CONFLICT -> DomainError.ConflictError(reason, fieldErrors)
            else -> DomainError.ServerError(reason, status.value)
        }
    }

    private fun UserDto?.toDomain(): User = User(
        id = this?.id.orEmpty(),
        username = this?.username.orEmpty(),
        firstName = this?.firstName.orEmpty(),
        lastName = this?.lastName.orEmpty(),
        email = this?.email.orEmpty(),
        phoneNumber = this?.phoneNumber,
        profilePictureUrl = this?.profilePictureUrl,
        provider = this?.provider,
        roles = this?.roles.orEmpty(),
    )

    private companion object {
        const val UNKNOWN_ERROR = "Something went wrong. Please try again."
        const val EMPTY_PAYLOAD = "The server returned an empty response."
        const val NO_ACTIVE_SESSION = "No active session."
        const val NOT_IMPLEMENTED = "Profile endpoint is not available yet."

        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_CONFLICT = 409
        const val HTTP_UNPROCESSABLE_ENTITY = 422
    }
}
