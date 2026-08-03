package com.iti.sheikh.presentation.di

import com.iti.sheikh.presentation.home.SheikhHomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sheikhPresentationModule = module {
    viewModel { SheikhHomeViewModel(get(), get(), get()) }
}
