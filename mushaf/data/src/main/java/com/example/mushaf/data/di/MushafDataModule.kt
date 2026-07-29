package com.example.mushaf.data.di

import com.example.mushaf.data.db.MushafAssetDataSource
import com.example.mushaf.data.db.QuranMetadataDataSource
import com.example.mushaf.data.db.QuranTextDataSource
import com.example.mushaf.data.download.FakeDownloadableResourceRepository
import com.example.mushaf.data.prefs.ReaderPreferencesDataStore
import com.example.mushaf.data.prefs.RecitationSettingsDataStore
import com.example.mushaf.data.repository.RecitationSchemaRepositoryImpl
import com.example.mushaf.data.repository.RecitationSettingsRepositoryImpl
import com.example.mushaf.domain.repository.RecitationSchemaRepository
import com.example.mushaf.domain.repository.RecitationSettingsRepository
import com.example.mushaf.data.recite.audio.AudioRecordPcmRecorder
import com.example.mushaf.data.recite.audio.PcmRecorder
import com.example.mushaf.data.recite.audio.WavDebugSink
import com.example.mushaf.data.recite.local.AndroidOnDeviceSpeechRecognizer
import com.example.mushaf.data.repository.LiveRecitationRepositoryImpl
import com.example.mushaf.data.repository.LocalWordCorpusRepositoryImpl
import com.example.mushaf.data.repository.MushafRepositoryImpl
import com.example.mushaf.data.repository.ReaderPreferencesRepositoryImpl
import com.example.mushaf.data.repository.RecitationCaptureRepositoryImpl
import com.example.mushaf.domain.repository.DownloadableResourceRepository
import com.example.mushaf.domain.repository.LiveRecitationRepository
import com.example.mushaf.domain.repository.LocalSpeechRecognizer
import com.example.mushaf.domain.repository.LocalWordCorpusRepository
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import com.example.mushaf.data.recite.remote.AiServiceApi
import com.example.mushaf.data.recite.remote.AiServiceConfig
import com.example.mushaf.data.recite.remote.LiveRecitationSocket
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

const val AI_SERVICE_CLIENT = "aiServiceClient"
const val SEARCH_CLIENT = "searchClient"

val mushafDataModule = module {
    single { MushafAssetDataSource(androidContext()) }
    single { QuranMetadataDataSource(androidContext()) }
    single { QuranTextDataSource(androidContext()) }
    single { com.example.mushaf.data.db.TafsirDataSource(androidContext()) }
    single { ReaderPreferencesDataStore(androidContext()) }

    // Remote Tafsir data source — uses the main authenticated Almahir HTTP client
    single { com.example.mushaf.data.tafsir.remote.TafsirRemoteDataSource(get()) }

    single { com.example.mushaf.data.search.remote.SearchApi(get(named(SEARCH_CLIENT))) }
    single { com.example.mushaf.data.search.remote.SemanticSearchRemoteDataSource(get()) }

    single<MushafRepository> { MushafRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<LocalWordCorpusRepository> { LocalWordCorpusRepositoryImpl(get(), get()) }
    single<LocalSpeechRecognizer> { AndroidOnDeviceSpeechRecognizer(androidContext()) }
    single<ReaderPreferencesRepository> { ReaderPreferencesRepositoryImpl(get()) }

    single<DownloadableResourceRepository> { FakeDownloadableResourceRepository() }

    single<PcmRecorder> { AudioRecordPcmRecorder() }
    single { WavDebugSink(androidContext()) }
    single<RecitationCaptureRepository> { RecitationCaptureRepositoryImpl(get(), get()) }

    
    
    single { AiServiceConfig() }
    single(named(AI_SERVICE_CLIENT)) {
        HttpClient(OkHttp) {
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false })
            }
            engine {
                config {
                    
                    
                    
                    
                    readTimeout(0, TimeUnit.MILLISECONDS)
                    pingInterval(20, TimeUnit.SECONDS)
                    
                    
                    
                    connectTimeout(4, TimeUnit.SECONDS)
                }
            }
        }
    }
    single { AiServiceApi(get(named(AI_SERVICE_CLIENT)), get()) }
    single { LiveRecitationSocket(get(named(AI_SERVICE_CLIENT)), get()) }
    single<LiveRecitationRepository> { LiveRecitationRepositoryImpl(get(), get()) }

    single { RecitationSettingsDataStore(androidContext()) }
    single<RecitationSettingsRepository> { RecitationSettingsRepositoryImpl(get()) }

    single<RecitationSchemaRepository> { RecitationSchemaRepositoryImpl(get()) }

    single(named(SEARCH_CLIENT)) { HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000L
            connectTimeoutMillis = 60_000L
            socketTimeoutMillis = 60_000L
        }
    } }
    single { com.example.mushaf.data.recitation.remote.QuranApi(get(named(SEARCH_CLIENT))) }
    single<com.example.mushaf.data.recitation.RecitationDataSource> { 
        com.example.mushaf.data.recitation.remote.RecitationRemoteDataSourceImpl(get()) 
    }
    single<com.example.mushaf.domain.repository.RecitationRepository> { 
        com.example.mushaf.data.repository.RecitationRepositoryImpl(get()) 
    }
}
