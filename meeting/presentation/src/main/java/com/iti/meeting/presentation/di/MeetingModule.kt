package com.iti.meeting.presentation.di

import com.iti.meeting.presentation.call.CallViewModel
import com.iti.meeting.presentation.call.audio.AudioRouteController
import com.iti.meeting.presentation.call.session.CallSessionController
import com.iti.meeting.domain.config.MeetingKitConfig
import com.iti.meeting.domain.repository.MeetingRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val meetingCallModule = module {
    single { AudioRouteController(context = androidContext()) }
    single {
        CallSessionController(
            appContext = androidContext(),
            config = get<MeetingKitConfig>(),
            repository = get<MeetingRepository>(),
            audioRoutes = get<AudioRouteController>(),
        )
    }
    viewModel { CallViewModel(controller = get<CallSessionController>()) }
}
