package com.iti.presentation.auth.navigation

import androidx.navigation3.runtime.NavKey
import com.iti.presentation.auth.otp.OtpFlow
import kotlinx.serialization.Serializable

/**
 * Destinations owned by the authentication flow.
 *
 * `NavKey` (Navigation 3) rather than a Navigation 2 route graph, so auth shares one back
 * stack with the rest of the app — see the mandated stack in AGENTS.md.
 */
sealed interface AuthRoute : NavKey {

    @Serializable
    data object Login : AuthRoute

    @Serializable
    data object Register : AuthRoute

    @Serializable
    data object ForgotPassword : AuthRoute

    @Serializable
    data class OtpVerify(
        val email: String,
        val flow: OtpFlow = OtpFlow.FORGOT_PASSWORD,
    ) : AuthRoute

    @Serializable
    data class ResetPassword(val email: String) : AuthRoute
}

