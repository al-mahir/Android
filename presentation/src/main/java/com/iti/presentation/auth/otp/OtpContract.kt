package com.iti.presentation.auth.otp

data class OtpState(
    val email: String = "",
    val otpCode: String = "",
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val timerSeconds: Int = 60,
    val canResend: Boolean = false
)

sealed class OtpIntent {
    data class InitEmail(val email: String) : OtpIntent()
    data class OtpChanged(val otp: String) : OtpIntent()
    object Submit : OtpIntent()
    object ResendOtp : OtpIntent()
    object TimerTick : OtpIntent()
}

sealed class OtpEffect {
    object NavigateToHome : OtpEffect()
    object NavigateToLogin : OtpEffect()
    data class ShowError(val message: String) : OtpEffect()
    object NavigateBack : OtpEffect()
}
