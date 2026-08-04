package com.iti.al_mahir

import android.app.Application
import com.example.mushaf.data.di.mushafDataModule
import com.example.mushaf.presentation.di.mushafPresentationModule
import com.iti.data.core.token.TokenStore
import com.iti.data.di.almahirDataModule
import com.iti.data.user.auth.di.authDataModule
import com.iti.domain.auth.di.authDomainModule
import com.iti.presentation.auth.di.authPresentationModule
import com.iti.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class AlMahirApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AlMahirApp)
            modules(
                almahirDataModule,
                presentationModule,
                mushafDataModule,
                mushafPresentationModule,
                authDataModule,
                authDomainModule,
                authPresentationModule,
                com.iti.meeting.data.di.meetingDataModule,
                com.iti.presentation.meetingrequest.di.meetingPresentationModule,
                                com.iti.meeting.presentation.di.meetingCallModule,
                org.koin.dsl.module {
                    single { 
                        com.iti.meeting.domain.config.MeetingKitConfig(
                            restBaseUrl = com.iti.al_mahir.BuildConfig.REST_BASE_URL,
                            wsBaseUrl = com.iti.al_mahir.BuildConfig.WS_BASE_URL,
                            agoraAppId = com.iti.al_mahir.BuildConfig.AGORA_APP_ID,
                            enableHttpLogging = com.iti.al_mahir.BuildConfig.DEBUG
                        ) 
                    }
                    single<com.iti.domain.auth.MeetingAuthTokenProvider> {
                        val tokenStore = org.koin.core.context.GlobalContext.get().get<TokenStore>()
                        val refresher = com.iti.data.core.token.TokenRefresher(
                            tokenStore = tokenStore,
                            refreshEndpoint = com.iti.data.core.network.AlmahirApi.Auth.REFRESH,
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





