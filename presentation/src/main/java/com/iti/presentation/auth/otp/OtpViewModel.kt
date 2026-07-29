package com.iti.presentation.auth.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.ForgotPasswordUseCase
import com.iti.domain.auth.usecase.VerifyOtpUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.auth.toUiText
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OtpViewModel(
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel(),
    StateHolder<OtpState> by DefaultStateHolder(OtpState()),
    EffectPublisher<OtpEffect> by DefaultEffectPublisher() {

    private var timerJob: Job? = null

    fun onIntent(intent: OtpIntent) {
        when (intent) {
            is OtpIntent.InitEmail -> {
                updateState { copy(email = intent.email) }
                startTimer()
            }
            is OtpIntent.OtpChanged -> updateState { copy(otpCode = intent.otp, isError = false) }
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
        val otp = currentState.otpCode
        val email = currentState.email

        if (otp.length != 6) {
            updateState { copy(isError = true) }
            return
        }

        updateState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val result = verifyOtpUseCase(email, otp)) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
                    sendEffect(OtpEffect.NavigateToLogin) // Navigate to login after successful reset verification
                }
                is Result.Error -> {
                    updateState { copy(isLoading = false) }
                    handleDomainError(result.error)
                }
            }
        }
    }

    private fun resendOtp() {
        updateState { copy(isLoading = true) }
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
        when (error) {
            is DomainError.ValidationError -> {
                updateState { copy(isError = true) }
            }
            else -> {
                sendEffect(OtpEffect.ShowError(error.toUiText()))
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
