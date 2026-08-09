package com.iti.presentation.auth.otp

import com.example.designsystem.text.UiText

/** Distinguishes why the user is entering an OTP. */
enum class OtpFlow {
    /** Forgot-password recovery — next screen is ResetPassword. */
    FORGOT_PASSWORD,

    /** Post-signup email verification — next step is auto-login → Home. */
    EMAIL_VERIFICATION,
}

data class OtpState(
    val email: String = "",
    val otpCode: String = "",
    val isError: Boolean = false,
    val errorMessage: UiText? = null,
    val isLoading: Boolean = false,
    val timerSeconds: Int = 60,
    val canResend: Boolean = false,
    val flow: OtpFlow = OtpFlow.FORGOT_PASSWORD,
)

sealed interface OtpIntent {
    data class InitEmail(val email: String, val flow: OtpFlow = OtpFlow.FORGOT_PASSWORD) : OtpIntent
    data class OtpChanged(val otp: String) : OtpIntent
    data object Submit : OtpIntent
    data object ResendOtp : OtpIntent
    data object TimerTick : OtpIntent
}

sealed interface OtpEffect {
    data class NavigateToResetPassword(val email: String) : OtpEffect
    data class NavigateToLogin(val message: UiText) : OtpEffect
    data class ShowError(val message: UiText) : OtpEffect
    data object NavigateBack : OtpEffect
}
