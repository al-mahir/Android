package com.iti.presentation.auth.resetpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.designsystem.text.UiText
import com.iti.domain.auth.model.AuthField
import com.iti.domain.auth.usecase.AuthValidators
import com.iti.domain.auth.usecase.ResetPasswordUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.R
import com.iti.presentation.auth.fieldError
import com.iti.presentation.auth.toUiText
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    private val resetPasswordUseCase: ResetPasswordUseCase,
) : ViewModel(),
    StateHolder<ResetPasswordState> by DefaultStateHolder(ResetPasswordState()),
    EffectPublisher<ResetPasswordEffect> by DefaultEffectPublisher() {

    fun onIntent(intent: ResetPasswordIntent) {
        when (intent) {
            is ResetPasswordIntent.InitEmail -> updateState { copy(email = intent.email) }
            is ResetPasswordIntent.NewPasswordChanged -> updateState {
                copy(newPassword = intent.password, newPasswordError = null)
            }
            is ResetPasswordIntent.ConfirmPasswordChanged -> updateState {
                copy(confirmPassword = intent.password, confirmPasswordError = null)
            }
            is ResetPasswordIntent.ToggleNewPasswordVisibility -> updateState {
                copy(isNewPasswordVisible = !isNewPasswordVisible)
            }
            is ResetPasswordIntent.ToggleConfirmPasswordVisibility -> updateState {
                copy(isConfirmPasswordVisible = !isConfirmPasswordVisible)
            }
            is ResetPasswordIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val state = currentState
        val newPassword = state.newPassword
        val confirmPassword = state.confirmPassword
        val email = state.email.trim()

        var hasError = false
        var newPassError: UiText? = null
        var confirmPassError: UiText? = null

        if (newPassword.isBlank()) {
            newPassError = UiText.Resource(R.string.auth_error_required)
            hasError = true
        } else if (!AuthValidators.isValidPassword(newPassword)) {
            newPassError = UiText.Resource(R.string.auth_error_weak_password)
            hasError = true
        }

        if (confirmPassword.isBlank()) {
            confirmPassError = UiText.Resource(R.string.auth_error_required)
            hasError = true
        } else if (newPassword != confirmPassword) {
            confirmPassError = UiText.Resource(R.string.auth_error_password_mismatch)
            hasError = true
        }

        if (hasError) {
            updateState {
                copy(
                    newPasswordError = newPassError,
                    confirmPasswordError = confirmPassError,
                )
            }
            return
        }

        updateState { copy(isLoading = true, newPasswordError = null, confirmPasswordError = null) }

        viewModelScope.launch {
            when (val result = resetPasswordUseCase(email, newPassword)) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
                    sendEffect(ResetPasswordEffect.ShowSuccess(UiText.Resource(R.string.auth_password_reset_success)))
                    sendEffect(ResetPasswordEffect.NavigateToLogin)
                }
                is Result.Error -> {
                    updateState { copy(isLoading = false) }
                    handleDomainError(result.error)
                }
            }
        }
    }

    private fun handleDomainError(error: DomainError) {
        val passError = error.fieldError(AuthField.PASSWORD)
        if (passError != null) {
            updateState { copy(newPasswordError = passError) }
            return
        }
        sendEffect(ResetPasswordEffect.ShowError(error.toUiText()))
    }
}
