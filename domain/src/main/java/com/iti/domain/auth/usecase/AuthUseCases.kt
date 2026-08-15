package com.iti.domain.auth.usecase

import com.iti.domain.auth.model.AuthData
import com.iti.domain.auth.model.AuthField
import com.iti.domain.auth.model.AuthValidationCode
import com.iti.domain.auth.model.User
import com.iti.domain.auth.repository.AuthRepository
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow

class ObserveAuthStateUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<Boolean> = repository.authState
}

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phoneNumber: String,
        gender: String
    ): Result<User> {
        val fieldErrors = buildMap {
            if (username.isBlank()) put(AuthField.USERNAME, AuthValidationCode.REQUIRED)
            if (firstName.isBlank()) put(AuthField.FIRST_NAME, AuthValidationCode.REQUIRED)
            if (lastName.isBlank()) put(AuthField.LAST_NAME, AuthValidationCode.REQUIRED)
            if (!AuthValidators.isValidPhoneNumber(phoneNumber)) {
                put(AuthField.PHONE_NUMBER, AuthValidationCode.INVALID_PHONE_NUMBER)
            }
            if (!AuthValidators.isValidEmail(email)) put(AuthField.EMAIL, AuthValidationCode.INVALID_EMAIL)
            if (!AuthValidators.isValidPassword(password)) put(AuthField.PASSWORD, AuthValidationCode.WEAK_PASSWORD)
            if (gender.isBlank()) put(AuthField.GENDER, AuthValidationCode.REQUIRED)
        }

        if (fieldErrors.isNotEmpty()) return validationFailure(fieldErrors)

        return repository.register(username, firstName, lastName, email, password, phoneNumber, gender)
    }
}

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthData> {
        val fieldErrors = buildMap {
            if (!AuthValidators.isValidEmail(email)) put(AuthField.EMAIL, AuthValidationCode.INVALID_EMAIL)
            if (password.isBlank()) put(AuthField.PASSWORD, AuthValidationCode.REQUIRED)
        }

        if (fieldErrors.isNotEmpty()) return validationFailure(fieldErrors)

        return repository.login(email, password)
    }
}

class LoginWithGoogleUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Result<AuthData> {
        return repository.loginWithGoogle(idToken)
    }
}

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.logout()
    }
}

class GetProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<User> {
        return repository.getProfile()
    }
}

class RefreshTokensUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.refreshTokens()
}

class ForgotPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (!AuthValidators.isValidEmail(email)) {
            return validationFailure(mapOf(AuthField.EMAIL to AuthValidationCode.INVALID_EMAIL))
        }
        return repository.forgotPassword(email)
    }
}

class VerifyOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, otp: String): Result<Unit> {
        if (otp.length != OTP_LENGTH) {
            return validationFailure(mapOf(AuthField.OTP to AuthValidationCode.INVALID_OTP))
        }
        return repository.verifyOtp(email, otp)
    }
}

class ResetPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(token: String, newPassword: String): Result<Unit> {
        if (!AuthValidators.isValidPassword(newPassword)) {
            return validationFailure(mapOf(AuthField.PASSWORD to AuthValidationCode.WEAK_PASSWORD))
        }
        return repository.resetPassword(token, newPassword)
    }
}

private const val OTP_LENGTH = 6

private fun validationFailure(fieldErrors: Map<String, String>): Result<Nothing> =
    Result.Error(DomainError.ValidationError(AuthValidationCode.VALIDATION_FAILED, fieldErrors))

