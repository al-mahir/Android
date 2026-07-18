package com.iti.presentation.di

import com.iti.domain.usecase.home.GetHomeSummaryUseCase
import com.iti.domain.usecase.home.JoinCircleUseCase
import com.iti.presentation.home.HomeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    factory { GetHomeSummaryUseCase(get()) }
    factory { JoinCircleUseCase(get()) }

    viewModel { HomeViewModel(get(), get()) }
}
