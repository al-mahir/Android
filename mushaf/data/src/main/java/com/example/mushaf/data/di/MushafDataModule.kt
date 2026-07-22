package com.example.mushaf.data.di

import com.example.mushaf.data.db.MushafAssetDataSource
import com.example.mushaf.data.download.FakeDownloadableResourceRepository
import com.example.mushaf.data.prefs.ReaderPreferencesDataStore
import com.example.mushaf.data.recite.audio.AudioRecordPcmRecorder
import com.example.mushaf.data.recite.audio.PcmRecorder
import com.example.mushaf.data.recite.audio.WavDebugSink
import com.example.mushaf.data.repository.LiveRecitationRepositoryImpl
import com.example.mushaf.data.repository.MushafRepositoryImpl
import com.example.mushaf.data.repository.ReaderPreferencesRepositoryImpl
import com.example.mushaf.data.repository.RecitationCaptureRepositoryImpl
import com.example.mushaf.domain.repository.DownloadableResourceRepository
import com.example.mushaf.domain.repository.LiveRecitationRepository
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import com.example.mushaf.data.recite.remote.AiServiceApi
import com.example.mushaf.data.recite.remote.AiServiceConfig
import com.example.mushaf.data.recite.remote.LiveRecitationSocket
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

/** Qualifier for the AI-service HTTP client, so it is never confused with the quran.com one. */
const val AI_SERVICE_CLIENT = "aiServiceClient"

val mushafDataModule = module {
    single { MushafAssetDataSource(androidContext()) }
    single { ReaderPreferencesDataStore(androidContext()) }

    single<MushafRepository> { MushafRepositoryImpl(get()) }
    single<ReaderPreferencesRepository> { ReaderPreferencesRepositoryImpl(get()) }

    single<DownloadableResourceRepository> { FakeDownloadableResourceRepository() }

    single<PcmRecorder> { AudioRecordPcmRecorder() }
    single { WavDebugSink(androidContext()) }
    single<RecitationCaptureRepository> { RecitationCaptureRepositoryImpl(get(), get()) }

    // Al-Mahir AI service. A second client on the OkHttp engine, because Ktor's Android engine
    // does not implement WebSockets at all — the live session cannot run on the client above.
    single { AiServiceConfig() }
    single(named(AI_SERVICE_CLIENT)) {
        HttpClient(OkHttp) {
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false })
            }
            engine {
                config {
                    // The server pushes only when the reciter pauses. Any read timeout would
                    // kill a healthy socket in the middle of a long ayah.
                    // TimeUnit overloads, not the java.time.Duration ones: those need API 26
                    // and this module ships to minSdk 24.
                    readTimeout(0, TimeUnit.MILLISECONDS)
                    pingInterval(20, TimeUnit.SECONDS)
                    // Short, so a wrong host fails in seconds rather than looking like a
                    // reciter who is being ignored. The default 10s x 3 reconnects is half a
                    // minute of silence before anything appears on screen.
                    connectTimeout(4, TimeUnit.SECONDS)
                }
            }
        }
    }
    single { AiServiceApi(get(named(AI_SERVICE_CLIENT)), get()) }
    single { LiveRecitationSocket(get(named(AI_SERVICE_CLIENT)), get()) }
    single<LiveRecitationRepository> { LiveRecitationRepositoryImpl(get(), get()) }

    single { HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    } }
    single { com.example.mushaf.data.recitation.remote.QuranApi(get()) }
    single<com.example.mushaf.data.recitation.RecitationDataSource> { 
        com.example.mushaf.data.recitation.remote.RecitationRemoteDataSourceImpl(get()) 
    }
    single<com.example.mushaf.domain.repository.RecitationRepository> { 
        com.example.mushaf.data.repository.RecitationRepositoryImpl(get()) 
    }
}
