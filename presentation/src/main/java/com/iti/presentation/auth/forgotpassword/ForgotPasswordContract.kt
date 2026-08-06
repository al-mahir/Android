package com.iti.presentation.auth.forgotpassword

import com.example.designsystem.text.UiText

data class ForgotPasswordState(
    val email: String = "",
    val emailError: UiText? = null,
    val isLoading: Boolean = false,
)

sealed interface ForgotPasswordIntent {
    data class EmailChanged(val email: String) : ForgotPasswordIntent
    data object Submit : ForgotPasswordIntent
}

sealed interface ForgotPasswordEffect {
    data class NavigateToOtpVerify(val email: String) : ForgotPasswordEffect
    data class ShowError(val message: UiText) : ForgotPasswordEffect
    data object NavigateBack : ForgotPasswordEffect
}
