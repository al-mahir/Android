package com.iti.presentation.auth.register

import com.iti.presentation.core.mvi.UiEffect
import com.iti.presentation.core.mvi.UiIntent
import com.iti.presentation.core.mvi.UiState

data class RegisterState(
    val username: String = "",
    val usernameError: String? = null,
    val firstName: String = "",
    val firstNameError: String? = null,
    val lastName: String = "",
    val lastNameError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val phoneNumber: String = "",
    val phoneNumberError: String? = null,
    val isLoading: Boolean = false
) : UiState

sealed class RegisterIntent : UiIntent {
    data class UsernameChanged(val username: String) : RegisterIntent()
    data class FirstNameChanged(val firstName: String) : RegisterIntent()
    data class LastNameChanged(val lastName: String) : RegisterIntent()
    data class EmailChanged(val email: String) : RegisterIntent()
    data class PasswordChanged(val password: String) : RegisterIntent()
    data class PhoneNumberChanged(val phoneNumber: String) : RegisterIntent()
    object TogglePasswordVisibility : RegisterIntent()
    object SubmitRegistration : RegisterIntent()
    object GoogleSignInClicked : RegisterIntent()
}

sealed class RegisterEffect : UiEffect {
    data class NavigateToOtpVerify(val email: String) : RegisterEffect()
    object NavigateToHome : RegisterEffect()
    data class ShowError(val message: String) : RegisterEffect()
}
