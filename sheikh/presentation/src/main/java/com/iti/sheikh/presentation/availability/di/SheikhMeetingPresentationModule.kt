package com.iti.sheikh.presentation.availability.di

import com.iti.sheikh.presentation.availability.AvailabilityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sheikhMeetingPresentationModule = module {
    viewModel { AvailabilityViewModel(repository = get(), currentUserProvider = get()) }
}

