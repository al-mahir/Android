package com.iti.presentation.auth.login

import com.iti.presentation.core.mvi.UiEffect
import com.iti.presentation.core.mvi.UiIntent
import com.iti.presentation.core.mvi.UiState

data class LoginState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
) : UiState

sealed class LoginIntent : UiIntent {
    data class EmailChanged(val email: String) : LoginIntent()
    data class PasswordChanged(val password: String) : LoginIntent()
    object TogglePasswordVisibility : LoginIntent()
    object SubmitLogin : LoginIntent()
    object GoogleSignInClicked : LoginIntent()
}

sealed class LoginEffect : UiEffect {
    object NavigateToHome : LoginEffect()
    data class ShowError(val message: String) : LoginEffect()
}
