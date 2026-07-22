package com.iti.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.model.AuthField
import com.iti.domain.auth.usecase.LoginUseCase
import com.iti.domain.auth.usecase.LoginWithGoogleUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.R
import com.iti.presentation.auth.fieldError
import com.iti.presentation.auth.toUiText
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.core.ui.UiText
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
            is LoginIntent.GoogleSignInClicked -> startGoogleSignIn()
            is LoginIntent.GoogleTokenReceived -> authenticate {
                loginWithGoogleUseCase(intent.idToken)
            }

            // The user backed out of the Google sheet; nothing to report.
            is LoginIntent.GoogleSignInDismissed -> updateState { copy(isLoading = false) }

            is LoginIntent.GoogleSignInUnavailable -> {
                updateState { copy(isLoading = false) }
                sendEffect(LoginEffect.ShowError(UiText.Resource(R.string.auth_error_google_unavailable)))
            }
        }
    }

    // The use case owns validation, so a malformed form never reaches the network.
    private fun submitLogin() {
        if (currentState.isLoading) return
        authenticate { loginUseCase(currentState.email.trim(), currentState.password) }
    }

    private fun startGoogleSignIn() {
        if (currentState.isLoading) return
        updateState { copy(isLoading = true) }
        sendEffect(LoginEffect.LaunchGoogleSignIn)
    }

    private fun authenticate(request: suspend () -> Result<*>) {
        updateState { copy(isLoading = true, emailError = null, passwordError = null) }

        viewModelScope.launch {
            val result = request()
            updateState { copy(isLoading = false) }

            when (result) {
                is Result.Success -> sendEffect(LoginEffect.NavigateToHome)
                is Result.Error -> handleDomainError(result.error)
            }
        }
    }

    private fun handleDomainError(error: DomainError) {
        val emailError = error.fieldError(AuthField.EMAIL)
        val passwordError = error.fieldError(AuthField.PASSWORD)

        if (emailError != null || passwordError != null) {
            updateState { copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        sendEffect(LoginEffect.ShowError(error.toUiText()))
    }
}
