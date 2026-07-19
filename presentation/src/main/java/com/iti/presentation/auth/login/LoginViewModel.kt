package com.iti.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.LoginUseCase
import com.iti.domain.auth.usecase.LoginWithGoogleUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase
) : ViewModel(),
    StateHolder<LoginState> by DefaultStateHolder(LoginState()),
    EffectPublisher<LoginEffect> by DefaultEffectPublisher() {

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> updateState { copy(email = intent.email, emailError = null) }
            is LoginIntent.PasswordChanged -> updateState { copy(password = intent.password, passwordError = null) }
            is LoginIntent.TogglePasswordVisibility -> updateState { copy(isPasswordVisible = !isPasswordVisible) }
            is LoginIntent.SubmitLogin -> submitLogin()
            is LoginIntent.GoogleSignInClicked -> handleGoogleSignIn()
        }
    }

    private fun submitLogin() {
        val email = currentState.email
        val password = currentState.password

        if (email.isBlank() || password.isBlank()) {
            updateState {
                copy(
                    emailError = if (email.isBlank()) "Email cannot be empty" else null,
                    passwordError = if (password.isBlank()) "Password cannot be empty" else null
                )
            }
            return
        }

        updateState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val result = loginUseCase(email, password)) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
                    sendEffect(LoginEffect.NavigateToHome)
                }
                is Result.Error -> {
                    updateState { copy(isLoading = false) }
                    handleDomainError(result.error)
                }
            }
        }
    }

    private fun handleGoogleSignIn() {
        // Placeholder for Google Sign In flow. Usually we launch an intent and get the ID token back.
        // For now, we will simulate receiving a mock token.
        updateState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val result = loginWithGoogleUseCase("mock_google_id_token")) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
                    sendEffect(LoginEffect.NavigateToHome)
                }
                is Result.Error -> {
                    updateState { copy(isLoading = false) }
                    sendEffect(LoginEffect.ShowError(result.error.toString()))
                }
            }
        }
    }

    private fun handleDomainError(error: DomainError) {
        when (error) {
            is DomainError.ValidationError -> {
                updateState {
                    copy(
                        emailError = error.fieldErrors["email"],
                        passwordError = error.fieldErrors["password"]
                    )
                }
            }
            is DomainError.ServerError -> {
                sendEffect(LoginEffect.ShowError(error.message))
            }
            is DomainError.NetworkError -> {
                sendEffect(LoginEffect.ShowError("Network error. Please try again."))
            }
            else -> {
                sendEffect(LoginEffect.ShowError("An unknown error occurred"))
            }
        }
    }
}
