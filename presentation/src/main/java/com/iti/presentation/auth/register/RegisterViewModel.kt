package com.iti.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.model.AuthField
import com.iti.domain.auth.usecase.LoginUseCase
import com.iti.domain.auth.usecase.LoginWithGoogleUseCase
import com.iti.domain.auth.usecase.RegisterUseCase
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

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase
) : ViewModel(),
    StateHolder<RegisterState> by DefaultStateHolder(RegisterState()),
    EffectPublisher<RegisterEffect> by DefaultEffectPublisher() {

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UsernameChanged -> updateState { copy(username = intent.username, usernameError = null) }
            is RegisterIntent.FirstNameChanged -> updateState { copy(firstName = intent.firstName, firstNameError = null) }
            is RegisterIntent.LastNameChanged -> updateState { copy(lastName = intent.lastName, lastNameError = null) }
            is RegisterIntent.EmailChanged -> updateState { copy(email = intent.email, emailError = null) }
            is RegisterIntent.PasswordChanged -> updateState { copy(password = intent.password, passwordError = null) }
            is RegisterIntent.PhoneNumberChanged -> updateState { copy(phoneNumber = intent.phoneNumber, phoneNumberError = null) }
            is RegisterIntent.GenderChanged -> updateState { copy(gender = intent.gender, genderError = null) }
            is RegisterIntent.TogglePasswordVisibility -> updateState { copy(isPasswordVisible = !isPasswordVisible) }
            is RegisterIntent.SubmitRegistration -> submitRegistration()
            is RegisterIntent.GoogleSignInClicked -> startGoogleSignIn()
            is RegisterIntent.GoogleTokenReceived -> signInWithGoogle(intent.idToken)

            is RegisterIntent.GoogleSignInDismissed -> updateState { copy(isLoading = false) }

            is RegisterIntent.GoogleSignInUnavailable -> {
                updateState { copy(isLoading = false) }
                sendEffect(
                    RegisterEffect.ShowError(UiText.Resource(R.string.auth_error_google_unavailable))
                )
            }
        }
    }


    private fun submitRegistration() {
        if (currentState.isLoading) return
        val email = currentState.email.trim()
        val password = currentState.password

        updateState { copy(isLoading = true).withoutFieldErrors() }

        viewModelScope.launch {
            val registration = registerUseCase(
                username = currentState.username.trim(),
                firstName = currentState.firstName.trim(),
                lastName = currentState.lastName.trim(),
                email = email,
                password = password,
                phoneNumber = currentState.phoneNumber.trim(),
                gender = currentState.gender,
            )

            if (registration is Result.Error) {
                updateState { copy(isLoading = false) }
                handleDomainError(registration.error)
                return@launch
            }

            val signIn = loginUseCase(email, password)
            updateState { copy(isLoading = false) }

            when (signIn) {
                is Result.Success -> sendEffect(RegisterEffect.NavigateToHome)
                is Result.Error -> sendEffect(
                    RegisterEffect.NavigateToLogin(
                        UiText.Resource(R.string.auth_error_registered_sign_in_failed)
                    )
                )
            }
        }
    }

    private fun startGoogleSignIn() {
        if (currentState.isLoading) return
        updateState { copy(isLoading = true) }
        sendEffect(RegisterEffect.LaunchGoogleSignIn)
    }

    private fun signInWithGoogle(idToken: String) {
        updateState { copy(isLoading = true) }

        viewModelScope.launch {
            val result = loginWithGoogleUseCase(idToken)
            updateState { copy(isLoading = false) }

            when (result) {
                is Result.Success -> sendEffect(RegisterEffect.NavigateToHome)
                is Result.Error -> sendEffect(RegisterEffect.ShowError(result.error.toUiText()))
            }
        }
    }

    private fun handleDomainError(error: DomainError) {
        updateState {
            copy(
                usernameError = error.fieldError(AuthField.USERNAME),
                firstNameError = error.fieldError(AuthField.FIRST_NAME),
                lastNameError = error.fieldError(AuthField.LAST_NAME),
                emailError = error.fieldError(AuthField.EMAIL),
                passwordError = error.fieldError(AuthField.PASSWORD),
                phoneNumberError = error.fieldError(AuthField.PHONE_NUMBER),
                genderError = error.fieldError(AuthField.GENDER),
            )
        }

        if (!currentState.hasFieldErrors()) sendEffect(RegisterEffect.ShowError(error.toUiText()))
    }

    private fun RegisterState.withoutFieldErrors() = copy(
        usernameError = null,
        firstNameError = null,
        lastNameError = null,
        emailError = null,
        passwordError = null,
        phoneNumberError = null,
        genderError = null,
    )

    private fun RegisterState.hasFieldErrors() = listOf(
        usernameError, firstNameError, lastNameError, emailError, passwordError, phoneNumberError, genderError,
    ).any { it != null }

}
