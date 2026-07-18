package com.iti.presentation.auth.otp

import com.iti.presentation.core.mvi.UiEffect
import com.iti.presentation.core.mvi.UiIntent
import com.iti.presentation.core.mvi.UiState

data class OtpState(
    val email: String = "",
    val otpCode: String = "",
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val timerSeconds: Int = 60,
    val canResend: Boolean = false
) : UiState

sealed class OtpIntent : UiIntent {
    data class InitEmail(val email: String) : OtpIntent()
    data class OtpChanged(val otp: String) : OtpIntent()
    object Submit : OtpIntent()
    object ResendOtp : OtpIntent()
    object TimerTick : OtpIntent()
}

sealed class OtpEffect : UiEffect {
    object NavigateToHome : OtpEffect()
    object NavigateToLogin : OtpEffect()
    data class ShowError(val message: String) : OtpEffect()
    object NavigateBack : OtpEffect()
}
