package com.iti.presentation.auth.resetpassword

import com.example.designsystem.text.UiText

data class ResetPasswordState(
    val email: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordError: UiText? = null,
    val confirmPasswordError: UiText? = null,
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
)

sealed interface ResetPasswordIntent {
    data class InitEmail(val email: String) : ResetPasswordIntent
    data class NewPasswordChanged(val password: String) : ResetPasswordIntent
    data class ConfirmPasswordChanged(val password: String) : ResetPasswordIntent
    data object ToggleNewPasswordVisibility : ResetPasswordIntent
    data object ToggleConfirmPasswordVisibility : ResetPasswordIntent
    data object Submit : ResetPasswordIntent
}

sealed interface ResetPasswordEffect {
    data object NavigateToLogin : ResetPasswordEffect
    data class ShowError(val message: UiText) : ResetPasswordEffect
    data class ShowSuccess(val message: UiText) : ResetPasswordEffect
    data object NavigateBack : ResetPasswordEffect
}
