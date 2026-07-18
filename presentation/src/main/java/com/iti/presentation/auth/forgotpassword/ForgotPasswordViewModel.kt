package com.iti.presentation.auth.forgotpassword

import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.ForgotPasswordUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.core.mvi.BaseViewModel
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : BaseViewModel<ForgotPasswordState, ForgotPasswordIntent, ForgotPasswordEffect>() {

    override fun createInitialState() = ForgotPasswordState()

    override fun handleIntent(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.EmailChanged -> setState { copy(email = intent.email, emailError = null) }
            is ForgotPasswordIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val email = currentState.email
        if (email.isBlank()) {
            setState { copy(emailError = "Email cannot be empty") }
            return
        }

        setState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val result = forgotPasswordUseCase(email)) {
                is Result.Success -> {
                    setState { copy(isLoading = false, isSuccess = true) }
                    // Navigate to OTP verify
                    setEffect { ForgotPasswordEffect.NavigateToOtpVerify(email) }
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
                setState { copy(emailError = error.fieldErrors["email"]) }
            }
            is DomainError.ServerError -> {
                setEffect { ForgotPasswordEffect.ShowError(error.message) }
            }
            is DomainError.NetworkError -> {
                setEffect { ForgotPasswordEffect.ShowError("Network error. Please try again.") }
            }
            else -> {
                setEffect { ForgotPasswordEffect.ShowError("An unknown error occurred") }
            }
        }
    }
}
