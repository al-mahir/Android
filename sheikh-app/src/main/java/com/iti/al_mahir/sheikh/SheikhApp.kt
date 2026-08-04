package com.iti.al_mahir.sheikh

import android.app.Application
import com.iti.data.core.token.TokenStore
import com.iti.data.di.almahirDataModule
import com.iti.data.sheikh.auth.di.sheikhAuthDataModule
import com.iti.domain.auth.di.authDomainModule
import com.iti.presentation.auth.di.authPresentationModule
import com.iti.presentation.di.presentationModule
import com.iti.sheikh.presentation.di.sheikhPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class SheikhApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SheikhApp)
            modules(
                almahirDataModule,
                presentationModule,
                sheikhPresentationModule,
                sheikhAuthDataModule,
                authDomainModule,
                authPresentationModule,
                com.iti.meeting.data.di.meetingDataModule,
                com.iti.presentation.meetingrequest.di.meetingPresentationModule,
                com.iti.sheikh.presentation.availability.di.sheikhMeetingPresentationModule,
                com.iti.meeting.presentation.di.meetingCallModule,
                org.koin.dsl.module {
                    single { 
                        com.iti.meeting.domain.config.MeetingKitConfig(
                            restBaseUrl = com.iti.al_mahir.sheikh.BuildConfig.REST_BASE_URL,
                            wsBaseUrl = com.iti.al_mahir.sheikh.BuildConfig.WS_BASE_URL,
                            agoraAppId = com.iti.al_mahir.sheikh.BuildConfig.AGORA_APP_ID,
                            enableHttpLogging = com.iti.al_mahir.sheikh.BuildConfig.DEBUG
                        ) 
                    }
                    single<com.iti.domain.auth.MeetingAuthTokenProvider> {
                        val tokenStore = org.koin.core.context.GlobalContext.get().get<TokenStore>()
                        val refresher = com.iti.data.core.token.TokenRefresher(
                            tokenStore = tokenStore,
                            refreshEndpoint = com.iti.data.core.network.AlmahirApi.Auth.Sheikh.REFRESH,
                        )
                        object : com.iti.domain.auth.MeetingAuthTokenProvider {
                            override suspend fun currentToken(): String? = tokenStore.getTokens()?.accessToken
                            override suspend fun refreshToken(): String? = refresher.refresh()?.accessToken
                            override suspend fun onAuthenticationExpired() = tokenStore.clear()
                        }
                    }
                    single<com.iti.domain.auth.MeetingCurrentUserProvider> {
                        com.iti.domain.auth.MeetingCurrentUserProvider {
                            org.koin.core.context.GlobalContext.get().get<TokenStore>().getUserId()
                        }
                    }
                }
            )
        }
    }
}




