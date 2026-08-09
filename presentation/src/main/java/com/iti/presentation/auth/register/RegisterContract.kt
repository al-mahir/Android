package com.iti.presentation.auth.register

import com.example.designsystem.text.UiText

data class RegisterState(
    val username: String = "",
    val usernameError: UiText? = null,
    val firstName: String = "",
    val firstNameError: UiText? = null,
    val lastName: String = "",
    val lastNameError: UiText? = null,
    val email: String = "",
    val emailError: UiText? = null,
    val password: String = "",
    val passwordError: UiText? = null,
    val isPasswordVisible: Boolean = false,
    val phoneNumber: String = "",
    val phoneNumberError: UiText? = null,
    val isLoading: Boolean = false
)

sealed class RegisterIntent {
    data class UsernameChanged(val username: String) : RegisterIntent()
    data class FirstNameChanged(val firstName: String) : RegisterIntent()
    data class LastNameChanged(val lastName: String) : RegisterIntent()
    data class EmailChanged(val email: String) : RegisterIntent()
    data class PasswordChanged(val password: String) : RegisterIntent()
    data class PhoneNumberChanged(val phoneNumber: String) : RegisterIntent()
    data object TogglePasswordVisibility : RegisterIntent()
    data object SubmitRegistration : RegisterIntent()
    data object GoogleSignInClicked : RegisterIntent()

    data class GoogleTokenReceived(val idToken: String) : RegisterIntent()

    data object GoogleSignInDismissed : RegisterIntent()
    data object GoogleSignInUnavailable : RegisterIntent()
}

sealed class RegisterEffect {
    data object NavigateToHome : RegisterEffect()

    data object LaunchGoogleSignIn : RegisterEffect()

    data class NavigateToLogin(val message: UiText) : RegisterEffect()

    /** Registration succeeded — verify email before signing in. */
    data class NavigateToOtpVerify(val email: String, val password: String) : RegisterEffect()

    data class ShowError(val message: UiText) : RegisterEffect()
}
