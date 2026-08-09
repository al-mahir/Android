package com.iti.presentation.auth

import com.iti.domain.auth.model.AuthValidationCode
import com.iti.domain.core.DomainError
import com.iti.presentation.R
import com.example.designsystem.text.UiText

internal fun DomainError.toUiText(): UiText = when (this) {
    is DomainError.ValidationError -> message.toUiText(R.string.auth_error_validation)
    is DomainError.ConflictError -> message.toUiText(R.string.auth_error_conflict)
    is DomainError.Unauthorized -> message.toUiText(R.string.auth_error_invalid_credentials)
    is DomainError.ServerError -> message.toUiText(R.string.error_generic)
    is DomainError.NetworkError -> UiText.Resource(R.string.error_network)
    is DomainError.NotFound -> message.toUiText(R.string.error_generic)
    is DomainError.Unknown -> UiText.Resource(R.string.error_generic)
}

internal fun DomainError.fieldError(field: String): UiText? =
    (this as? DomainError.ValidationError)?.fieldErrors?.get(field)?.toUiText(R.string.error_generic)

private fun String?.toUiText(fallback: Int): UiText = when {
    isNullOrBlank() -> UiText.Resource(fallback)
    else -> VALIDATION_CODE_STRINGS[this]?.let(UiText::Resource) ?: UiText.Dynamic(this)
}

private val VALIDATION_CODE_STRINGS = mapOf(
    AuthValidationCode.VALIDATION_FAILED to R.string.auth_error_validation,
    AuthValidationCode.REQUIRED to R.string.auth_error_required,
    AuthValidationCode.INVALID_EMAIL to R.string.auth_error_invalid_email,
    AuthValidationCode.WEAK_PASSWORD to R.string.auth_error_weak_password,
    AuthValidationCode.INVALID_PHONE_NUMBER to R.string.auth_error_invalid_phone_number,
    AuthValidationCode.INVALID_OTP to R.string.auth_error_invalid_otp,
    AuthValidationCode.PASSWORD_MISMATCH to R.string.auth_error_password_mismatch,
)
