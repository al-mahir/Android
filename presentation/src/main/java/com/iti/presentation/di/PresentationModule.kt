package com.iti.presentation.di

import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.domain.usecase.reading.GetReadingProgressUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.presentation.home.HomeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    factory { GetCurrentUserUseCase(get()) }
    factory { GetReadingProgressUseCase(get()) }
    factory { GetSheikhsUseCase(get()) }
    factory { GetStudyCirclesUseCase(get()) }
    factory { JoinStudyCircleUseCase(get()) }

    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
}
