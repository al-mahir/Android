package com.iti.presentation.auth.di

import com.iti.presentation.auth.forgotpassword.ForgotPasswordViewModel
import com.iti.presentation.auth.login.LoginViewModel
import com.iti.presentation.auth.otp.OtpViewModel
import com.iti.presentation.auth.register.RegisterViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val authPresentationModule = module {
    viewModel { LoginViewModel(get(), get()) }
    viewModel { RegisterViewModel(get(), get()) }
    viewModel { ForgotPasswordViewModel(get()) }
    viewModel { OtpViewModel(get(), get()) }
}
