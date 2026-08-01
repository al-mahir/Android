package com.iti.presentation.meetingrequest.di

import com.iti.presentation.meetingrequest.browse.SheikhBrowseViewModel
import com.iti.presentation.meetingrequest.request.MeetingRequestViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val meetingPresentationModule = module {
    viewModel { SheikhBrowseViewModel(repository = get()) }
    viewModel { MeetingRequestViewModel(repository = get()) }
}
