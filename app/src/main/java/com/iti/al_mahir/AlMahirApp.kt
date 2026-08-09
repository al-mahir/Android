package com.iti.al_mahir

import android.app.Application
import com.example.mushaf.data.di.mushafDataModule
import com.example.mushaf.presentation.di.mushafPresentationModule
import com.iti.data.core.token.TokenStore
import com.iti.domain.core.getOrNull
import com.iti.data.di.almahirDataModule
import com.iti.data.payment.di.paymentDataModule
import com.iti.data.user.auth.di.authDataModule
import com.iti.domain.auth.MeetingAuthTokenProvider
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

private const val JWT_EXPIRY_LEEWAY_MS = 60_000L

private fun String.isJwtExpired(): Boolean {
    val payload = split('.').getOrNull(1) ?: return false
    return runCatching {
        val padded = payload + when (payload.length % 4) {
            2 -> "=="
            3 -> "="
            else -> ""
        }
        val decoded = String(
            android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
        )
        val exp = org.json.JSONObject(decoded).optLong("exp", 0L)
        exp > 0L && exp * 1000L <= System.currentTimeMillis() + JWT_EXPIRY_LEEWAY_MS
    }.getOrDefault(false)
}

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
                    single<MeetingAuthTokenProvider> {
                        val tokenStore = GlobalContext.get().get<TokenStore>()
                        val refresher = com.iti.data.core.token.TokenRefresher(
                            tokenStore = tokenStore,
                            refreshEndpoint = com.iti.data.core.network.AlmahirApi.Auth.REFRESH,
                        )
                        object : MeetingAuthTokenProvider {
                            override suspend fun currentToken(): String? = tokenStore.getTokens()?.accessToken
                            override suspend fun refreshToken(): String? = refresher.refresh()?.accessToken
                            override suspend fun onAuthenticationExpired() = tokenStore.clear()
                        }
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





