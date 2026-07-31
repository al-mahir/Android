package com.example.mushaf.data.di

import com.example.mushaf.data.core.di.AI_SERVICE_CLIENT
import com.example.mushaf.data.core.di.SEARCH_CLIENT
import com.example.mushaf.data.core.di.mushafNetworkModule
import com.example.mushaf.data.db.MushafAssetDataSource
import com.example.mushaf.data.db.QuranMetadataDataSource
import com.example.mushaf.data.db.QuranTextDataSource
import com.example.mushaf.data.download.FakeDownloadableResourceRepository
import com.example.mushaf.data.prefs.ReaderPreferencesDataStore
import com.example.mushaf.data.prefs.RecitationSettingsDataStore
import com.example.mushaf.data.recite.audio.AudioRecordPcmRecorder
import com.example.mushaf.data.recite.audio.PcmRecorder
import com.example.mushaf.data.recite.audio.WavDebugSink
import com.example.mushaf.data.recite.local.AndroidOnDeviceSpeechRecognizer
import com.example.mushaf.data.recite.remote.AiServiceApi
import com.example.mushaf.data.recite.remote.AiServiceConfig
import com.example.mushaf.data.recite.remote.LiveRecitationSocket
import com.example.mushaf.data.repository.LiveRecitationRepositoryImpl
import com.example.mushaf.data.repository.MushafPreferencesRepositoryImpl
import com.example.mushaf.data.repository.MushafRepositoryImpl
import com.example.mushaf.data.repository.ReadingProgressRepositoryImpl
import com.example.mushaf.data.repository.RecitationApiRepositoryImpl
import com.example.mushaf.data.repository.RecitationCaptureRepositoryImpl
import com.example.mushaf.domain.repository.DownloadableResourceRepository
import com.example.mushaf.domain.repository.LiveRecitationRepository
import com.example.mushaf.domain.repository.LocalSpeechRecognizer
import com.example.mushaf.domain.repository.LocalWordCorpusRepository
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import com.example.mushaf.domain.repository.RecitationRepository
import com.example.mushaf.domain.repository.RecitationSchemaRepository
import com.example.mushaf.domain.repository.RecitationSettingsRepository
import com.iti.domain.repository.ReadingProgressRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val mushafDataModule = module {
    includes(mushafNetworkModule)

    single { MushafAssetDataSource(androidContext()) }
    single { QuranMetadataDataSource(androidContext()) }
    single { QuranTextDataSource(androidContext()) }
    single { com.example.mushaf.data.db.TafsirDataSource(androidContext()) }
    single { ReaderPreferencesDataStore(androidContext()) }

    // Remote Tafsir data source — uses the main authenticated Almahir HTTP client
    single { com.example.mushaf.data.tafsir.remote.TafsirRemoteDataSource(get(named("almahir-http-client"))) }
    single { com.example.mushaf.data.tafsir.local.TafsirDownloadManager(androidContext()) }
    single { com.example.mushaf.data.tafsir.local.TafsirLocalJsonDataSource(get()) }

    single { com.example.mushaf.data.search.remote.SearchApi(get(named(SEARCH_CLIENT))) }
    single { com.example.mushaf.data.search.remote.SemanticSearchRemoteDataSource(get()) }

    single { MushafRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<MushafRepository> { get<MushafRepositoryImpl>() }
    single<LocalWordCorpusRepository> { get<MushafRepositoryImpl>() }
    single<LocalSpeechRecognizer> { AndroidOnDeviceSpeechRecognizer(androidContext()) }
    single<ReadingProgressRepository> { ReadingProgressRepositoryImpl(get()) }

    single { RecitationSettingsDataStore(androidContext()) }
    single { MushafPreferencesRepositoryImpl(get(), get()) }
    single<ReaderPreferencesRepository> { get<MushafPreferencesRepositoryImpl>() }
    single<RecitationSettingsRepository> { get<MushafPreferencesRepositoryImpl>() }

    single<DownloadableResourceRepository> { FakeDownloadableResourceRepository() }

    single<PcmRecorder> { AudioRecordPcmRecorder() }
    single { WavDebugSink(androidContext()) }
    single<RecitationCaptureRepository> { RecitationCaptureRepositoryImpl(get(), get()) }

    single { AiServiceConfig() }
    single { AiServiceApi(get(named(AI_SERVICE_CLIENT)), get()) }
    single { LiveRecitationSocket(get(named(AI_SERVICE_CLIENT)), get()) }
    single<LiveRecitationRepository> { LiveRecitationRepositoryImpl(get(), get()) }

    single { com.example.mushaf.data.recitation.remote.QuranApi(get(named(SEARCH_CLIENT))) }
    single<com.example.mushaf.data.recitation.RecitationDataSource> {
        com.example.mushaf.data.recitation.remote.RecitationRemoteDataSourceImpl(get())
    }
    single { RecitationApiRepositoryImpl(get(), get()) }
    single<RecitationRepository> { get<RecitationApiRepositoryImpl>() }
    single<RecitationSchemaRepository> { get<RecitationApiRepositoryImpl>() }
}
