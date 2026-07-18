package com.iti.presentation.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
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
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

// Define routes
@Serializable data object AuthGraph
@Serializable data object LoginRoute
@Serializable data object RegisterRoute
@Serializable data object ForgotPasswordRoute
@Serializable data class OtpVerifyRoute(val email: String)

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onNavigateToHome: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    navigation<AuthGraph>(startDestination = LoginRoute) {
        
        composable<LoginRoute> {
            val viewModel: LoginViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()
            
            LaunchedEffect(Unit) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        is LoginEffect.NavigateToHome -> onNavigateToHome()
                        is LoginEffect.ShowError -> onShowMessage(effect.message)
                    }
                }
            }
            
            LoginScreen(
                state = state,
                onIntent = viewModel::sendIntent,
                onNavigateToRegister = { navController.navigate(RegisterRoute) },
                onNavigateToForgotPassword = { navController.navigate(ForgotPasswordRoute) }
            )
        }

        composable<RegisterRoute> {
            val viewModel: RegisterViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()
            
            LaunchedEffect(Unit) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        is RegisterEffect.NavigateToOtpVerify -> navController.navigate(OtpVerifyRoute(effect.email))
                        is RegisterEffect.NavigateToHome -> onNavigateToHome()
                        is RegisterEffect.ShowError -> onShowMessage(effect.message)
                    }
                }
            }
            
            RegisterScreen(
                state = state,
                onIntent = viewModel::sendIntent,
                onNavigateToLogin = { navController.navigate(LoginRoute) {
                    popUpTo(LoginRoute) { inclusive = false }
                } }
            )
        }

        composable<ForgotPasswordRoute> {
            val viewModel: ForgotPasswordViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()
            
            LaunchedEffect(Unit) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        is ForgotPasswordEffect.NavigateToOtpVerify -> navController.navigate(OtpVerifyRoute(effect.email))
                        is ForgotPasswordEffect.NavigateBack -> navController.popBackStack()
                        is ForgotPasswordEffect.ShowError -> onShowMessage(effect.message)
                    }
                }
            }
            
            ForgotPasswordScreen(
                state = state,
                onIntent = viewModel::sendIntent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<OtpVerifyRoute> { backStackEntry ->
            val route: OtpVerifyRoute = backStackEntry.toRoute()
            val viewModel: OtpViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()
            
            LaunchedEffect(Unit) {
                viewModel.sendIntent(OtpIntent.InitEmail(route.email))
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        is OtpEffect.NavigateToLogin -> navController.navigate(LoginRoute) {
                            popUpTo(AuthGraph) { inclusive = false }
                        }
                        is OtpEffect.NavigateToHome -> onNavigateToHome()
                        is OtpEffect.NavigateBack -> navController.popBackStack()
                        is OtpEffect.ShowError -> onShowMessage(effect.message)
                    }
                }
            }
            
            OtpScreen(
                state = state,
                onIntent = viewModel::sendIntent
            )
        }
    }
}
