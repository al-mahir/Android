package com.iti.presentation.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.model.AuthField
import com.iti.domain.auth.usecase.ForgotPasswordUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.R
import com.iti.presentation.auth.fieldError
import com.iti.presentation.auth.toUiText
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.example.designsystem.text.UiText
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
            updateState { copy(emailError = UiText.Resource(R.string.auth_error_required)) }
            return
        }

        updateState { copy(isLoading = true, emailError = null) }

        viewModelScope.launch {
            when (val result = forgotPasswordUseCase(email)) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
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
        val emailError = error.fieldError(AuthField.EMAIL)
        if (emailError != null) {
            updateState { copy(emailError = emailError) }
            return
        }
        sendEffect(ForgotPasswordEffect.ShowError(error.toUiText()))
    }
}
