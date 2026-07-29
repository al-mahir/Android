package com.iti.presentation.auth.forgotpassword

import com.example.designsystem.text.UiText

data class ForgotPasswordState(
    val email: String = "",
    val emailError: UiText? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

sealed class ForgotPasswordIntent {
    data class EmailChanged(val email: String) : ForgotPasswordIntent()
    object Submit : ForgotPasswordIntent()
}

sealed class ForgotPasswordEffect {
    data class NavigateToOtpVerify(val email: String) : ForgotPasswordEffect()
    data class ShowError(val message: UiText) : ForgotPasswordEffect()
    object NavigateBack : ForgotPasswordEffect()
}
