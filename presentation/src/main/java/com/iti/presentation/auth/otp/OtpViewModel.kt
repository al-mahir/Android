package com.iti.presentation.auth.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.ForgotPasswordUseCase
import com.iti.domain.auth.usecase.LoginUseCase
import com.iti.domain.auth.usecase.VerifyOtpUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.R
import com.iti.presentation.auth.fieldError
import com.iti.presentation.auth.toUiText
import com.example.designsystem.text.UiText
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OtpViewModel(
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val loginUseCase: LoginUseCase,
) : ViewModel(),
    StateHolder<OtpState> by DefaultStateHolder(OtpState()),
    EffectPublisher<OtpEffect> by DefaultEffectPublisher() {

    private var timerJob: Job? = null

    /**
     * Credentials stashed from the register screen so we can auto-login
     * after successful email verification. Only set for [OtpFlow.EMAIL_VERIFICATION].
     */
    private var pendingPassword: String? = null

    fun setCredentials(password: String) {
        pendingPassword = password
    }

    fun onIntent(intent: OtpIntent) {
        when (intent) {
            is OtpIntent.InitEmail -> {
                updateState { copy(email = intent.email, flow = intent.flow) }
                startTimer()
            }
            is OtpIntent.OtpChanged -> updateState { copy(otpCode = intent.otp, isError = false, errorMessage = null) }
            is OtpIntent.Submit -> submit()
            is OtpIntent.ResendOtp -> resendOtp()
            is OtpIntent.TimerTick -> {
                val current = currentState.timerSeconds
                if (current > 0) {
                    updateState { copy(timerSeconds = current - 1) }
                } else {
                    updateState { copy(canResend = true) }
                    timerJob?.cancel()
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        updateState { copy(timerSeconds = 60, canResend = false) }
        timerJob = viewModelScope.launch {
            while (currentState.timerSeconds > 0) {
                delay(1000)
                onIntent(OtpIntent.TimerTick)
            }
        }
    }

    private fun submit() {
        val otp = currentState.otpCode.trim()
        val email = currentState.email.trim()

        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            updateState {
                copy(
                    isError = true,
                    errorMessage = UiText.Resource(R.string.auth_error_invalid_otp)
                )
            }
            return
        }

        updateState { copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = verifyOtpUseCase(email, otp)) {
                is Result.Success -> {
                    when (currentState.flow) {
                        OtpFlow.FORGOT_PASSWORD -> {
                            updateState { copy(isLoading = false) }
                            sendEffect(OtpEffect.NavigateToResetPassword(email))
                        }
                        OtpFlow.EMAIL_VERIFICATION -> {
                            attemptAutoLogin(email)
                        }
                    }
                }
                is Result.Error -> {
                    updateState { copy(isLoading = false) }
                    handleDomainError(result.error)
                }
            }
        }
    }

    /**
     * After email verification succeeds, attempt auto-login with the stashed
     * credentials. If login fails (shouldn't normally), fall back to the
     * login screen with a success message.
     */
    private suspend fun attemptAutoLogin(email: String) {
        val password = pendingPassword
        if (password != null) {
            when (loginUseCase(email, password)) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
                    sendEffect(
                        OtpEffect.NavigateToLogin(
                            UiText.Resource(R.string.auth_email_verified)
                        )
                    )
                }
                is Result.Error -> {
                    updateState { copy(isLoading = false) }
                    sendEffect(
                        OtpEffect.NavigateToLogin(
                            UiText.Resource(R.string.auth_email_verified_sign_in)
                        )
                    )
                }
            }
        } else {
            updateState { copy(isLoading = false) }
            sendEffect(
                OtpEffect.NavigateToLogin(
                    UiText.Resource(R.string.auth_email_verified_sign_in)
                )
            )
        }
    }

    private fun resendOtp() {
        updateState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = forgotPasswordUseCase(currentState.email)) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
                    startTimer()
                }
                is Result.Error -> {
                    updateState { copy(isLoading = false) }
                    handleDomainError(result.error)
                }
            }
        }
    }

    private fun handleDomainError(error: DomainError) {
        val errorUiText = when (error) {
            is DomainError.ValidationError -> {
                error.fieldError("otp") ?: error.toUiText()
            }
            is DomainError.NotFound, is DomainError.ServerError, is DomainError.Unauthorized -> {
                val mapped = error.toUiText()
                if (mapped is UiText.Dynamic) {
                    UiText.Resource(R.string.auth_otp_verification_failed)
                } else {
                    mapped
                }
            }
            is DomainError.NetworkError -> UiText.Resource(R.string.error_network)
            else -> error.toUiText()
        }
        updateState {
            copy(
                isError = true,
                errorMessage = errorUiText,
            )
        }
        sendEffect(OtpEffect.ShowError(errorUiText))
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
