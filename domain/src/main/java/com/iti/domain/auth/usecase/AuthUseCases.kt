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
        phoneNumber: String
    ): Result<User> {
        val trimmedUsername = username.trim()
        val trimmedFirstName = firstName.trim()
        val trimmedLastName = lastName.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phoneNumber.trim()

        val fieldErrors = buildMap {
            if (trimmedUsername.isBlank()) put(AuthField.USERNAME, AuthValidationCode.REQUIRED)
            if (trimmedFirstName.isBlank()) put(AuthField.FIRST_NAME, AuthValidationCode.REQUIRED)
            if (trimmedLastName.isBlank()) put(AuthField.LAST_NAME, AuthValidationCode.REQUIRED)
            if (trimmedPhone.isBlank()) {
                put(AuthField.PHONE_NUMBER, AuthValidationCode.REQUIRED)
            } else if (!AuthValidators.isValidPhoneNumber(trimmedPhone)) {
                put(AuthField.PHONE_NUMBER, AuthValidationCode.INVALID_PHONE_NUMBER)
            }
            if (trimmedEmail.isBlank()) {
                put(AuthField.EMAIL, AuthValidationCode.REQUIRED)
            } else if (!AuthValidators.isValidEmail(trimmedEmail)) {
                put(AuthField.EMAIL, AuthValidationCode.INVALID_EMAIL)
            }
            if (password.isBlank()) {
                put(AuthField.PASSWORD, AuthValidationCode.REQUIRED)
            } else if (!AuthValidators.isValidPassword(password)) {
                put(AuthField.PASSWORD, AuthValidationCode.WEAK_PASSWORD)
            }
        }

        if (fieldErrors.isNotEmpty()) return validationFailure(fieldErrors)

        return repository.register(
            username = trimmedUsername,
            firstName = trimmedFirstName,
            lastName = trimmedLastName,
            email = trimmedEmail,
            password = password,
            phoneNumber = trimmedPhone
        )
    }
}

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthData> {
        val trimmedEmail = email.trim()
        val fieldErrors = buildMap {
            if (trimmedEmail.isBlank()) {
                put(AuthField.EMAIL, AuthValidationCode.REQUIRED)
            } else if (!AuthValidators.isValidEmail(trimmedEmail)) {
                put(AuthField.EMAIL, AuthValidationCode.INVALID_EMAIL)
            }
            if (password.isBlank()) put(AuthField.PASSWORD, AuthValidationCode.REQUIRED)
        }

        if (fieldErrors.isNotEmpty()) return validationFailure(fieldErrors)

        return repository.login(trimmedEmail, password)
    }
}

class LoginWithGoogleUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Result<AuthData> {
        val trimmed = idToken.trim()
        if (trimmed.isBlank()) {
            return Result.Error(DomainError.ValidationError(AuthValidationCode.REQUIRED))
        }
        return repository.loginWithGoogle(trimmed)
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
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return validationFailure(mapOf(AuthField.EMAIL to AuthValidationCode.REQUIRED))
        }
        if (!AuthValidators.isValidEmail(trimmed)) {
            return validationFailure(mapOf(AuthField.EMAIL to AuthValidationCode.INVALID_EMAIL))
        }
        return repository.forgotPassword(trimmed)
    }
}

class VerifyOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, otp: String): Result<Unit> {
        val trimmedEmail = email.trim()
        val trimmedOtp = otp.trim()
        val fieldErrors = buildMap {
            if (trimmedEmail.isBlank()) {
                put(AuthField.EMAIL, AuthValidationCode.REQUIRED)
            } else if (!AuthValidators.isValidEmail(trimmedEmail)) {
                put(AuthField.EMAIL, AuthValidationCode.INVALID_EMAIL)
            }
            if (trimmedOtp.isBlank()) {
                put(AuthField.OTP, AuthValidationCode.REQUIRED)
            } else if (trimmedOtp.length != OTP_LENGTH || !trimmedOtp.all { it.isDigit() }) {
                put(AuthField.OTP, AuthValidationCode.INVALID_OTP)
            }
        }
        if (fieldErrors.isNotEmpty()) return validationFailure(fieldErrors)
        return repository.verifyOtp(trimmedEmail, trimmedOtp)
    }
}

class ResetPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, newPassword: String): Result<Unit> {
        val trimmedEmail = email.trim()
        val fieldErrors = buildMap {
            if (trimmedEmail.isBlank()) {
                put(AuthField.EMAIL, AuthValidationCode.REQUIRED)
            } else if (!AuthValidators.isValidEmail(trimmedEmail)) {
                put(AuthField.EMAIL, AuthValidationCode.INVALID_EMAIL)
            }
            if (newPassword.isBlank()) {
                put(AuthField.PASSWORD, AuthValidationCode.REQUIRED)
            } else if (!AuthValidators.isValidPassword(newPassword)) {
                put(AuthField.PASSWORD, AuthValidationCode.WEAK_PASSWORD)
            }
        }
        if (fieldErrors.isNotEmpty()) return validationFailure(fieldErrors)
        return repository.resetPassword(trimmedEmail, newPassword)
    }
}

private const val OTP_LENGTH = 6

private fun validationFailure(fieldErrors: Map<String, String>): Result<Nothing> =
    Result.Error(DomainError.ValidationError(AuthValidationCode.VALIDATION_FAILED, fieldErrors))

