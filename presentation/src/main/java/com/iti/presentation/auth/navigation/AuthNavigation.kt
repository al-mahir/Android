package com.iti.presentation.auth.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.iti.presentation.auth.forgotpassword.ForgotPasswordEffect
import com.iti.presentation.auth.forgotpassword.ForgotPasswordScreen
import com.iti.presentation.auth.forgotpassword.ForgotPasswordViewModel
import com.iti.presentation.auth.login.LoginEffect
import com.iti.presentation.auth.login.LoginScreen
import com.iti.presentation.auth.login.LoginViewModel
import com.iti.presentation.auth.otp.OtpEffect
import com.iti.presentation.auth.otp.OtpIntent
import com.iti.presentation.auth.otp.OtpScreen
import com.iti.presentation.auth.otp.OtpViewModel
import com.iti.presentation.auth.register.RegisterEffect
import com.iti.presentation.auth.register.RegisterScreen
import com.iti.presentation.auth.register.RegisterViewModel
import com.iti.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel

/**
 * Registers the authentication destinations on the app's single Navigation 3 back stack.
 *
 * Navigation is hoisted: this graph never owns a controller, it only reports where it wants
 * to go. `:app` decides what that means — in particular [onAuthenticated], which is how the
 * auth flow hands over to the main app without knowing the main graph exists.
 *
 * @param onNavigate push a destination onto the back stack
 * @param onBack pop the current destination
 * @param onAuthenticated the user is signed in; replace auth with the app's main entry point
 * @param onShowMessage surface a transient error to the user
 */
fun EntryProviderScope<NavKey>.authEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    entry<AuthRoute.Login> {
        val viewModel: LoginViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        ObserveEffect(viewModel.effect) { effect ->
            when (effect) {
                is LoginEffect.NavigateToHome -> onAuthenticated()
                is LoginEffect.ShowError -> onShowMessage(effect.message)
            }
        }

        LoginScreen(
            state = state,
            onIntent = viewModel::onIntent,
            onNavigateToRegister = { onNavigate(AuthRoute.Register) },
            onNavigateToForgotPassword = { onNavigate(AuthRoute.ForgotPassword) },
        )
    }

    entry<AuthRoute.Register> {
        val viewModel: RegisterViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        ObserveEffect(viewModel.effect) { effect ->
            when (effect) {
                is RegisterEffect.NavigateToOtpVerify ->
                    onNavigate(AuthRoute.OtpVerify(effect.email))

                is RegisterEffect.NavigateToHome -> onAuthenticated()
                is RegisterEffect.ShowError -> onShowMessage(effect.message)
            }
        }

        RegisterScreen(
            state = state,
            onIntent = viewModel::onIntent,
            // Register is always pushed from Login, so "go to login" is a pop.
            onNavigateToLogin = onBack,
        )
    }

    entry<AuthRoute.ForgotPassword> {
        val viewModel: ForgotPasswordViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        ObserveEffect(viewModel.effect) { effect ->
            when (effect) {
                is ForgotPasswordEffect.NavigateToOtpVerify ->
                    onNavigate(AuthRoute.OtpVerify(effect.email))

                is ForgotPasswordEffect.NavigateBack -> onBack()
                is ForgotPasswordEffect.ShowError -> onShowMessage(effect.message)
            }
        }

        ForgotPasswordScreen(
            state = state,
            onIntent = viewModel::onIntent,
            onNavigateBack = onBack,
        )
    }

    entry<AuthRoute.OtpVerify> { route ->
        val viewModel: OtpViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        // Seed the ViewModel with the address the code was sent to.
        LaunchedEffect(route.email) {
            viewModel.onIntent(OtpIntent.InitEmail(route.email))
        }

        ObserveEffect(viewModel.effect) { effect ->
            when (effect) {
                is OtpEffect.NavigateToLogin -> onNavigate(AuthRoute.Login)
                is OtpEffect.NavigateToHome -> onAuthenticated()
                is OtpEffect.NavigateBack -> onBack()
                is OtpEffect.ShowError -> onShowMessage(effect.message)
            }
        }

        OtpScreen(
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}
