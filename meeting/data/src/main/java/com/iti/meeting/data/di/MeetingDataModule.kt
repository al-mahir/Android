package com.iti.meeting.data.di

import com.iti.meeting.domain.config.MeetingKitConfig
import com.iti.meeting.data.local.PendingMeetingRequestStore
import com.iti.meeting.data.remote.MeetingApi
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.meeting.data.repository.MeetingRepositoryImpl
import com.iti.meeting.data.remote.createMeetingHttpClient
import com.iti.meeting.data.realtime.StompClient
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val meetingDataModule = module {
    single<HttpClient> {
        val config = get<MeetingKitConfig>()
        createMeetingHttpClient(
            tokenProvider = get(),
            restBaseUrl = config.restBaseUrl,
            enableLogging = config.enableHttpLogging,
        )
    }

    factory { 
        val config = get<MeetingKitConfig>()
        StompClient(httpClient = get(), wsUrl = config.wsBaseUrl, tokenProvider = get()) 
    }

    single { MeetingApi(httpClient = get()) }
    single { PendingMeetingRequestStore(context = androidContext()) }
    single<MeetingRepository> {
        MeetingRepositoryImpl(api = get(), stompClient = get(), pendingRequestStore = get())
    }
}


