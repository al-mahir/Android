package com.iti.sheikh.presentation.availability.di

import com.iti.sheikh.presentation.availability.AvailabilityViewModel
import com.iti.sheikh.presentation.availability.SheikhAvailabilityController
import com.iti.sheikh.presentation.availability.service.IncomingRequestRinger
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sheikhMeetingPresentationModule = module {
    single {
        SheikhAvailabilityController(
            appContext = androidContext(),
            repository = get(),
            currentUserProvider = get(),
        )
    }
    single { IncomingRequestRinger(context = androidContext()) }
    viewModel { AvailabilityViewModel(controller = get()) }
}

