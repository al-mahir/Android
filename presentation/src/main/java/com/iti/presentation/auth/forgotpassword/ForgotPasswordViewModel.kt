package com.iti.presentation.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.ForgotPasswordUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel(),
    StateHolder<ForgotPasswordState> by DefaultStateHolder(ForgotPasswordState()),
    EffectPublisher<ForgotPasswordEffect> by DefaultEffectPublisher() {

    fun onIntent(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.EmailChanged -> updateState { copy(email = intent.email, emailError = null) }
            is ForgotPasswordIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val email = currentState.email
        if (email.isBlank()) {
            updateState { copy(emailError = "Email cannot be empty") }
            return
        }

        updateState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val result = forgotPasswordUseCase(email)) {
                is Result.Success -> {
                    updateState { copy(isLoading = false, isSuccess = true) }
                    // Navigate to OTP verify
                    sendEffect(ForgotPasswordEffect.NavigateToOtpVerify(email))
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
                updateState { copy(emailError = error.fieldErrors["email"]) }
            }
            is DomainError.ServerError -> {
                sendEffect(ForgotPasswordEffect.ShowError(error.message))
            }
            is DomainError.NetworkError -> {
                sendEffect(ForgotPasswordEffect.ShowError("Network error. Please try again."))
            }
            else -> {
                sendEffect(ForgotPasswordEffect.ShowError("An unknown error occurred"))
            }
        }
    }
}
