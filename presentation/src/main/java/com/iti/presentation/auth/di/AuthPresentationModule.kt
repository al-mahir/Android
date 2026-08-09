package com.iti.presentation.auth.di

import com.iti.presentation.BuildConfig
import com.iti.presentation.auth.forgotpassword.ForgotPasswordViewModel
import com.iti.presentation.auth.login.LoginViewModel
import com.iti.presentation.auth.otp.OtpViewModel
import com.iti.presentation.auth.register.RegisterViewModel
import com.iti.presentation.auth.resetpassword.ResetPasswordViewModel
import com.iti.presentation.auth.session.SessionViewModel
import com.iti.presentation.core.platform.CredentialManagerGoogleIdTokenProvider
import com.iti.presentation.core.platform.GoogleIdTokenProvider
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authPresentationModule = module {
    single<GoogleIdTokenProvider> {
        CredentialManagerGoogleIdTokenProvider(BuildConfig.GOOGLE_WEB_CLIENT_ID)
    }

    viewModel { SessionViewModel(get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { RegisterViewModel(get(), get(), get()) }
    viewModel { ForgotPasswordViewModel(get()) }
    viewModel { OtpViewModel(get(), get(), get()) }
    viewModel { ResetPasswordViewModel(get()) }
}
