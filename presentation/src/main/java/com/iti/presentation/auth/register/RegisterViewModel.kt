package com.iti.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.LoginWithGoogleUseCase
import com.iti.domain.auth.usecase.RegisterUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
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
            is RegisterIntent.TogglePasswordVisibility -> updateState { copy(isPasswordVisible = !isPasswordVisible) }
            is RegisterIntent.SubmitRegistration -> submitRegistration()
            is RegisterIntent.GoogleSignInClicked -> handleGoogleSignIn()
        }
    }

    private fun submitRegistration() {
        val state = currentState
        
        updateState { copy(isLoading = true) }
        
        viewModelScope.launch {
            val result = registerUseCase(
                username = state.username,
                firstName = state.firstName,
                lastName = state.lastName,
                email = state.email,
                password = state.password,
                phoneNumber = state.phoneNumber
            )
            
            when (result) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
                    // Navigate to OTP verify passing the email
                    sendEffect(RegisterEffect.NavigateToOtpVerify(state.email))
                }
                is Result.Error -> {
                    updateState { copy(isLoading = false) }
                    handleDomainError(result.error)
                }
            }
        }
    }

    private fun handleGoogleSignIn() {
        updateState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val result = loginWithGoogleUseCase("mock_google_id_token")) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
                    sendEffect(RegisterEffect.NavigateToHome)
                }
                is Result.Error -> {
                    updateState { copy(isLoading = false) }
                    sendEffect(RegisterEffect.ShowError(result.error.toString()))
                }
            }
        }
    }

    private fun handleDomainError(error: DomainError) {
        when (error) {
            is DomainError.ValidationError -> {
                updateState {
                    copy(
                        usernameError = error.fieldErrors["username"],
                        firstNameError = error.fieldErrors["firstName"],
                        lastNameError = error.fieldErrors["lastName"],
                        emailError = error.fieldErrors["email"],
                        passwordError = error.fieldErrors["password"],
                        phoneNumberError = error.fieldErrors["phoneNumber"]
                    )
                }
            }
            is DomainError.ServerError -> {
                sendEffect(RegisterEffect.ShowError(error.message))
            }
            is DomainError.NetworkError -> {
                sendEffect(RegisterEffect.ShowError("Network error. Please try again."))
            }
            else -> {
                sendEffect(RegisterEffect.ShowError("An unknown error occurred"))
            }
        }
    }
}
