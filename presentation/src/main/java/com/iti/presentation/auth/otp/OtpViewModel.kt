package com.iti.presentation.auth.otp

import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.ForgotPasswordUseCase
import com.iti.domain.auth.usecase.VerifyOtpUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.core.mvi.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OtpViewModel(
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : BaseViewModel<OtpState, OtpIntent, OtpEffect>() {

    private var timerJob: Job? = null

    override fun createInitialState() = OtpState()

    override fun handleIntent(intent: OtpIntent) {
        when (intent) {
            is OtpIntent.InitEmail -> {
                setState { copy(email = intent.email) }
                startTimer()
            }
            is OtpIntent.OtpChanged -> setState { copy(otpCode = intent.otp, isError = false) }
            is OtpIntent.Submit -> submit()
            is OtpIntent.ResendOtp -> resendOtp()
            is OtpIntent.TimerTick -> {
                val current = currentState.timerSeconds
                if (current > 0) {
                    setState { copy(timerSeconds = current - 1) }
                } else {
                    setState { copy(canResend = true) }
                    timerJob?.cancel()
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        setState { copy(timerSeconds = 60, canResend = false) }
        timerJob = viewModelScope.launch {
            while (currentState.timerSeconds > 0) {
                delay(1000)
                sendIntent(OtpIntent.TimerTick)
            }
        }
    }

    private fun submit() {
        val otp = currentState.otpCode
        val email = currentState.email

        if (otp.length != 6) {
            setState { copy(isError = true) }
            return
        }

        setState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val result = verifyOtpUseCase(email, otp)) {
                is Result.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect { OtpEffect.NavigateToLogin } // Navigate to login after successful reset verification
                }
                is Result.Error -> {
                    setState { copy(isLoading = false) }
                    handleDomainError(result.error)
                }
            }
        }
    }

    private fun resendOtp() {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = forgotPasswordUseCase(currentState.email)) {
                is Result.Success -> {
                    setState { copy(isLoading = false) }
                    startTimer()
                }
                is Result.Error -> {
                    setState { copy(isLoading = false) }
                    handleDomainError(result.error)
                }
            }
        }
    }

    private fun handleDomainError(error: DomainError) {
        when (error) {
            is DomainError.ValidationError -> {
                setState { copy(isError = true) }
            }
            is DomainError.ServerError -> {
                setEffect { OtpEffect.ShowError(error.message) }
            }
            is DomainError.NetworkError -> {
                setEffect { OtpEffect.ShowError("Network error. Please try again.") }
            }
            else -> {
                setEffect { OtpEffect.ShowError("An unknown error occurred") }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
