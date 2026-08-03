package com.iti.al_mahir

import android.app.Application
import com.example.mushaf.data.di.mushafDataModule
import com.example.mushaf.presentation.di.mushafPresentationModule
import com.iti.data.core.token.TokenStore
import com.iti.domain.core.getOrNull
import com.iti.data.di.almahirDataModule
import com.iti.data.user.auth.di.authDataModule
import com.iti.domain.auth.di.authDomainModule
import com.iti.presentation.auth.di.authPresentationModule
import com.iti.presentation.di.presentationModule
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
                        com.iti.domain.auth.MeetingAuthTokenProvider {
                            val tokenStore: TokenStore = org.koin.core.context.GlobalContext.get().get()
                            val current = tokenStore.getTokens()
                            when {
                                current == null -> null
                                !current.accessToken.isJwtExpired() -> current.accessToken
                                else -> {
                                    val authRepository: com.iti.domain.auth.repository.AuthRepository =
                                        org.koin.core.context.GlobalContext.get().get()
                                    val refreshed = authRepository.refreshTokens().getOrNull()?.accessToken
                                    android.util.Log.d(
                                        "MeetingAuth",
                                        "access token expired; refresh ${if (refreshed != null) "succeeded" else "failed"}"
                                    )
                                    refreshed ?: current.accessToken
                                }
                            }
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





