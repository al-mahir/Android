package com.iti.presentation.auth.register

import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.LoginWithGoogleUseCase
import com.iti.domain.auth.usecase.RegisterUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.core.mvi.BaseViewModel
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase
) : BaseViewModel<RegisterState, RegisterIntent, RegisterEffect>() {

    override fun createInitialState() = RegisterState()

    override fun handleIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UsernameChanged -> setState { copy(username = intent.username, usernameError = null) }
            is RegisterIntent.FirstNameChanged -> setState { copy(firstName = intent.firstName, firstNameError = null) }
            is RegisterIntent.LastNameChanged -> setState { copy(lastName = intent.lastName, lastNameError = null) }
            is RegisterIntent.EmailChanged -> setState { copy(email = intent.email, emailError = null) }
            is RegisterIntent.PasswordChanged -> setState { copy(password = intent.password, passwordError = null) }
            is RegisterIntent.PhoneNumberChanged -> setState { copy(phoneNumber = intent.phoneNumber, phoneNumberError = null) }
            is RegisterIntent.TogglePasswordVisibility -> setState { copy(isPasswordVisible = !isPasswordVisible) }
            is RegisterIntent.SubmitRegistration -> submitRegistration()
            is RegisterIntent.GoogleSignInClicked -> handleGoogleSignIn()
        }
    }

    private fun submitRegistration() {
        val state = currentState
        
        setState { copy(isLoading = true) }
        
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
                    setState { copy(isLoading = false) }
                    // Navigate to OTP verify passing the email
                    setEffect { RegisterEffect.NavigateToOtpVerify(state.email) }
                }
                is Result.Error -> {
                    setState { copy(isLoading = false) }
                    handleDomainError(result.error)
                }
            }
        }
    }

    private fun handleGoogleSignIn() {
        setState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val result = loginWithGoogleUseCase("mock_google_id_token")) {
                is Result.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect { RegisterEffect.NavigateToHome }
                }
                is Result.Error -> {
                    setState { copy(isLoading = false) }
                    setEffect { RegisterEffect.ShowError(result.error.toString()) }
                }
            }
        }
    }

    private fun handleDomainError(error: DomainError) {
        when (error) {
            is DomainError.ValidationError -> {
                setState {
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
                setEffect { RegisterEffect.ShowError(error.message) }
            }
            is DomainError.NetworkError -> {
                setEffect { RegisterEffect.ShowError("Network error. Please try again.") }
            }
            else -> {
                setEffect { RegisterEffect.ShowError("An unknown error occurred") }
            }
        }
    }
}
