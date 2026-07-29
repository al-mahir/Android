package com.iti.meeting.presentation.di

import com.iti.meeting.presentation.call.CallViewModel
import com.iti.meeting.domain.config.MeetingKitConfig
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val meetingCallModule = module {
    viewModel { CallViewModel(config = get<MeetingKitConfig>()) }
}

