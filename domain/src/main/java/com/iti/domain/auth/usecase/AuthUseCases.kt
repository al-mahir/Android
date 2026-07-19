package com.iti.domain.auth.usecase

import com.iti.domain.auth.model.AuthData
import com.iti.domain.auth.model.User
import com.iti.domain.auth.repository.AuthRepository
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phoneNumber: String
    ): Result<User> {
        val fieldErrors = mutableMapOf<String, String>()
        
        if (!AuthValidators.isValidEmail(email)) {
            fieldErrors["email"] = "Invalid email format"
        }
        if (!AuthValidators.isValidPassword(password)) {
            fieldErrors["password"] = "Password must contain at least 8 characters, including upper, lower, number, and special character"
        }
        if (username.isBlank()) fieldErrors["username"] = "Username cannot be empty"
        if (firstName.isBlank()) fieldErrors["firstName"] = "First name cannot be empty"
        if (lastName.isBlank()) fieldErrors["lastName"] = "Last name cannot be empty"
        if (phoneNumber.isBlank()) fieldErrors["phoneNumber"] = "Phone number cannot be empty"

        if (fieldErrors.isNotEmpty()) {
            return Result.Error(DomainError.ValidationError("Validation failed", fieldErrors))
        }

        return repository.register(username, firstName, lastName, email, password, phoneNumber)
    }
}

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthData> {
        val fieldErrors = mutableMapOf<String, String>()
        
        if (!AuthValidators.isValidEmail(email)) {
            fieldErrors["email"] = "Invalid email format"
        }
        if (password.isBlank()) {
            fieldErrors["password"] = "Password cannot be empty"
        }
        if (fieldErrors.isNotEmpty()) {
            return Result.Error(DomainError.ValidationError("Validation failed", fieldErrors))
        }
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
            return Result.Error(DomainError.ValidationError("Validation failed", mapOf("email" to "Invalid email format")))
        }
        return repository.forgotPassword(email)
    }
}

class VerifyOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, otp: String): Result<Unit> {
        if (otp.length != 6) {
             return Result.Error(DomainError.ValidationError("Validation failed", mapOf("otp" to "OTP must be 6 digits")))
        }
        return repository.verifyOtp(email, otp)
    }
}

class ResetPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(token: String, newPassword: String): Result<Unit> {
        if (!AuthValidators.isValidPassword(newPassword)) {
            return Result.Error(DomainError.ValidationError("Validation failed", mapOf("password" to "Password does not meet requirements")))
        }
        return repository.resetPassword(token, newPassword)
    }
}
