package com.iti.domain.auth.di

import com.iti.domain.auth.usecase.ForgotPasswordUseCase
import com.iti.domain.auth.usecase.GetProfileUseCase
import com.iti.domain.auth.usecase.LoginUseCase
import com.iti.domain.auth.usecase.LoginWithGoogleUseCase
import com.iti.domain.auth.usecase.LogoutUseCase
import com.iti.domain.auth.usecase.RefreshTokensUseCase
import com.iti.domain.auth.usecase.RegisterUseCase
import com.iti.domain.auth.usecase.ResetPasswordUseCase
import com.iti.domain.auth.usecase.VerifyOtpUseCase
import org.koin.dsl.module

val authDomainModule = module {
    factory { RegisterUseCase(get()) }
    factory { LoginUseCase(get()) }
    factory { LoginWithGoogleUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { GetProfileUseCase(get()) }
    factory { RefreshTokensUseCase(get()) }
    factory { ForgotPasswordUseCase(get()) }
    factory { VerifyOtpUseCase(get()) }
    factory { ResetPasswordUseCase(get()) }
}
