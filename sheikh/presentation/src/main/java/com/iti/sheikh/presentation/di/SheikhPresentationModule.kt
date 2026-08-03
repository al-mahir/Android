package com.iti.sheikh.presentation.di

import com.iti.sheikh.presentation.circle.SheikhCircleListViewModel
import com.iti.sheikh.presentation.circle.SheikhCircleManageViewModel
import com.iti.sheikh.presentation.circle.SheikhCreateCircleViewModel
import com.iti.sheikh.presentation.home.SheikhHomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sheikhPresentationModule = module {
    viewModel { SheikhHomeViewModel(get(), get(), get()) }
    viewModel { SheikhCircleListViewModel(get()) }
    viewModel { SheikhCreateCircleViewModel(get()) }
    viewModel { (circleId: String) -> SheikhCircleManageViewModel(circleId, get()) }
}
