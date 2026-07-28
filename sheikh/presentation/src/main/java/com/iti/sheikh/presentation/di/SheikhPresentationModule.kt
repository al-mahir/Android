package com.iti.sheikh.presentation.di

import com.iti.domain.usecase.sheikh.ObserveMyAvailabilityUseCase
import com.iti.domain.usecase.sheikh.SetMyAvailabilityUseCase
import com.iti.sheikh.presentation.home.SheikhHomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sheikhPresentationModule = module {
    factory { ObserveMyAvailabilityUseCase(get()) }
    factory { SetMyAvailabilityUseCase(get()) }

    viewModel { SheikhHomeViewModel(get(), get(), get()) }
}
