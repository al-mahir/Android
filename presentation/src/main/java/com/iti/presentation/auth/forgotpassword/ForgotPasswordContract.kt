package com.iti.presentation.auth.forgotpassword

import com.iti.presentation.core.mvi.UiEffect
import com.iti.presentation.core.mvi.UiIntent
import com.iti.presentation.core.mvi.UiState

data class ForgotPasswordState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
) : UiState

sealed class ForgotPasswordIntent : UiIntent {
    data class EmailChanged(val email: String) : ForgotPasswordIntent()
    object Submit : ForgotPasswordIntent()
}

sealed class ForgotPasswordEffect : UiEffect {
    data class NavigateToOtpVerify(val email: String) : ForgotPasswordEffect()
    data class ShowError(val message: String) : ForgotPasswordEffect()
    object NavigateBack : ForgotPasswordEffect()
}
