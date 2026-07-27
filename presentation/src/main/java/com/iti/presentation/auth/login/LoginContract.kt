package com.iti.presentation.auth.login

import com.example.designsystem.text.UiText

data class LoginState(
    val email: String = "",
    val emailError: UiText? = null,
    val password: String = "",
    val passwordError: UiText? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
)

sealed class LoginIntent {
    data class EmailChanged(val email: String) : LoginIntent()
    data class PasswordChanged(val password: String) : LoginIntent()
    data object TogglePasswordVisibility : LoginIntent()
    data object SubmitLogin : LoginIntent()
    data object GoogleSignInClicked : LoginIntent()

    data class GoogleTokenReceived(val idToken: String) : LoginIntent()

    data object GoogleSignInDismissed : LoginIntent()
    data object GoogleSignInUnavailable : LoginIntent()
}

sealed class LoginEffect {
    data object NavigateToHome : LoginEffect()

    data object LaunchGoogleSignIn : LoginEffect()

    data class ShowError(val message: UiText) : LoginEffect()
}
