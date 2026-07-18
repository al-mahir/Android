package com.iti.presentation.auth.login

import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.LoginUseCase
import com.iti.domain.auth.usecase.LoginWithGoogleUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.core.mvi.BaseViewModel
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase
) : BaseViewModel<LoginState, LoginIntent, LoginEffect>() {

    override fun createInitialState() = LoginState()

    override fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> setState { copy(email = intent.email, emailError = null) }
            is LoginIntent.PasswordChanged -> setState { copy(password = intent.password, passwordError = null) }
            is LoginIntent.TogglePasswordVisibility -> setState { copy(isPasswordVisible = !isPasswordVisible) }
            is LoginIntent.SubmitLogin -> submitLogin()
            is LoginIntent.GoogleSignInClicked -> handleGoogleSignIn()
        }
    }

    private fun submitLogin() {
        val email = currentState.email
        val password = currentState.password

        if (email.isBlank() || password.isBlank()) {
            setState {
                copy(
                    emailError = if (email.isBlank()) "Email cannot be empty" else null,
                    passwordError = if (password.isBlank()) "Password cannot be empty" else null
                )
            }
            return
        }

        setState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val result = loginUseCase(email, password)) {
                is Result.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect { LoginEffect.NavigateToHome }
                }
                is Result.Error -> {
                    setState { copy(isLoading = false) }
                    handleDomainError(result.error)
                }
            }
        }
    }

    private fun handleGoogleSignIn() {
        // Placeholder for Google Sign In flow. Usually we launch an intent and get the ID token back.
        // For now, we will simulate receiving a mock token.
        setState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val result = loginWithGoogleUseCase("mock_google_id_token")) {
                is Result.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect { LoginEffect.NavigateToHome }
                }
                is Result.Error -> {
                    setState { copy(isLoading = false) }
                    setEffect { LoginEffect.ShowError(result.error.toString()) }
                }
            }
        }
    }

    private fun handleDomainError(error: DomainError) {
        when (error) {
            is DomainError.ValidationError -> {
                setState {
                    copy(
                        emailError = error.fieldErrors["email"],
                        passwordError = error.fieldErrors["password"]
                    )
                }
            }
            is DomainError.ServerError -> {
                setEffect { LoginEffect.ShowError(error.message) }
            }
            is DomainError.NetworkError -> {
                setEffect { LoginEffect.ShowError("Network error. Please try again.") }
            }
            else -> {
                setEffect { LoginEffect.ShowError("An unknown error occurred") }
            }
        }
    }
}
