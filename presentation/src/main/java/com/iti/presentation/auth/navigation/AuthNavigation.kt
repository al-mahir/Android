package com.iti.presentation.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.iti.presentation.auth.forgotpassword.ForgotPasswordEffect
import com.iti.presentation.auth.forgotpassword.ForgotPasswordScreen
import com.iti.presentation.auth.forgotpassword.ForgotPasswordViewModel
import com.iti.presentation.auth.login.LoginEffect
import com.iti.presentation.auth.login.LoginIntent
import com.iti.presentation.auth.login.LoginScreen
import com.iti.presentation.auth.login.LoginViewModel
import com.iti.presentation.auth.otp.OtpEffect
import com.iti.presentation.auth.otp.OtpIntent
import com.iti.presentation.auth.otp.OtpScreen
import com.iti.presentation.auth.otp.OtpViewModel
import com.iti.presentation.auth.register.RegisterEffect
import com.iti.presentation.auth.register.RegisterIntent
import com.iti.presentation.auth.register.RegisterScreen
import com.iti.presentation.auth.register.RegisterViewModel
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.core.platform.GoogleIdTokenProvider
import com.iti.presentation.core.platform.GoogleIdTokenResult
import com.example.designsystem.text.resolve
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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

@Composable
private fun rememberGoogleSignInLauncher(
    onResult: (GoogleIdTokenResult) -> Unit,
): () -> Unit {
    val provider: GoogleIdTokenProvider = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)

    return remember(provider, context, scope) {
        {
            scope.launch {
                val result = provider.requestIdToken(context)
                if (result is GoogleIdTokenResult.Failed) {
                    android.util.Log.e("GoogleSignIn", "Google Sign In Failed", result.cause)
                }
                currentOnResult(result)
            }
            Unit
        }
    }
}

fun EntryProviderScope<NavKey>.authEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    entry<AuthRoute.Login> {
        val viewModel: LoginViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        val requestGoogleIdToken = rememberGoogleSignInLauncher { result ->
            viewModel.onIntent(
                when (result) {
                    is GoogleIdTokenResult.Success -> LoginIntent.GoogleTokenReceived(result.idToken)
                    is GoogleIdTokenResult.Cancelled -> LoginIntent.GoogleSignInDismissed
                    else -> LoginIntent.GoogleSignInUnavailable
                }
            )
        }

        ObserveEffect(viewModel.effect) { effect ->
            when (effect) {
                is LoginEffect.NavigateToHome -> onAuthenticated()
                is LoginEffect.LaunchGoogleSignIn -> requestGoogleIdToken()
                is LoginEffect.ShowError -> onShowMessage(effect.message.resolve(context))
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
        val context = LocalContext.current

        val requestGoogleIdToken = rememberGoogleSignInLauncher { result ->
            viewModel.onIntent(
                when (result) {
                    is GoogleIdTokenResult.Success ->
                        RegisterIntent.GoogleTokenReceived(result.idToken)

                    is GoogleIdTokenResult.Cancelled -> RegisterIntent.GoogleSignInDismissed
                    else -> RegisterIntent.GoogleSignInUnavailable
                }
            )
        }

        ObserveEffect(viewModel.effect) { effect ->
            when (effect) {
                is RegisterEffect.NavigateToHome -> onAuthenticated()
                is RegisterEffect.LaunchGoogleSignIn -> requestGoogleIdToken()

                // Register is always pushed from Login, so returning there is a pop.
                is RegisterEffect.NavigateToLogin -> {
                    onShowMessage(effect.message.resolve(context))
                    onBack()
                }

                is RegisterEffect.ShowError -> onShowMessage(effect.message.resolve(context))
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
        val context = LocalContext.current

        ObserveEffect(viewModel.effect) { effect ->
            when (effect) {
                is ForgotPasswordEffect.NavigateToOtpVerify ->
                    onNavigate(AuthRoute.OtpVerify(effect.email))

                is ForgotPasswordEffect.NavigateBack -> onBack()
                is ForgotPasswordEffect.ShowError -> onShowMessage(effect.message.resolve(context))
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
        val context = LocalContext.current

        // Seed the ViewModel with the address the code was sent to.
        LaunchedEffect(route.email) {
            viewModel.onIntent(OtpIntent.InitEmail(route.email))
        }

        ObserveEffect(viewModel.effect) { effect ->
            when (effect) {
                is OtpEffect.NavigateToLogin -> onNavigate(AuthRoute.Login)
                is OtpEffect.NavigateToHome -> onAuthenticated()
                is OtpEffect.NavigateBack -> onBack()
                is OtpEffect.ShowError -> onShowMessage(effect.message.resolve(context))
            }
        }

        OtpScreen(
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}
