package com.iti.al_mahir

import android.app.Application
import com.example.mushaf.data.di.mushafDataModule
import com.example.mushaf.presentation.di.mushafPresentationModule
import com.iti.data.core.token.TokenStore
import com.iti.domain.core.getOrNull
import com.iti.data.di.almahirDataModule
import com.iti.data.payment.di.paymentDataModule
import com.iti.data.user.auth.di.authDataModule
import com.iti.domain.auth.MeetingCurrentUserProvider
import com.iti.domain.auth.di.authDomainModule
import com.iti.domain.payment.di.paymentDomainModule
import com.iti.meeting.data.di.meetingDataModule
import com.iti.meeting.domain.config.MeetingKitConfig
import com.iti.meeting.presentation.di.meetingCallModule
import com.iti.presentation.auth.di.authPresentationModule
import com.iti.presentation.di.presentationModule
import com.iti.presentation.meetingrequest.di.meetingPresentationModule
import com.iti.presentation.payment.checkout.di.paymentPresentationModule
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
                paymentDataModule,
                paymentDomainModule,
                paymentPresentationModule,
                meetingDataModule,
                meetingPresentationModule,
                                meetingCallModule,
                org.koin.dsl.module {
                    single {
                        MeetingKitConfig(
                            restBaseUrl = BuildConfig.REST_BASE_URL,
                            wsBaseUrl = BuildConfig.WS_BASE_URL,
                            agoraAppId = BuildConfig.AGORA_APP_ID,
                            enableHttpLogging = BuildConfig.DEBUG
                        ) 
                    }
                    single<MeetingCurrentUserProvider> {
                        MeetingCurrentUserProvider {
                            GlobalContext.get().get<TokenStore>().getUserId()
                        }
                    }
                }
            )
        }
    }
}





