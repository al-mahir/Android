package com.example.mushaf.data.di

import com.example.mushaf.data.db.MushafAssetDataSource
import com.example.mushaf.data.db.QuranMetadataDataSource
import com.example.mushaf.data.db.QuranTextDataSource
import com.example.mushaf.data.download.FakeDownloadableResourceRepository
import com.example.mushaf.data.prefs.ReaderPreferencesDataStore
import com.example.mushaf.data.repository.MushafRepositoryImpl
import com.example.mushaf.data.repository.ReaderPreferencesRepositoryImpl
import com.example.mushaf.domain.repository.DownloadableResourceRepository
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

import io.ktor.client.plugins.HttpTimeout

val mushafDataModule = module {
    single { MushafAssetDataSource(androidContext()) }
    single { QuranMetadataDataSource(androidContext()) }
    single { QuranTextDataSource(androidContext()) }
    single { com.example.mushaf.data.db.TafsirDataSource(androidContext()) }
    single { ReaderPreferencesDataStore(androidContext()) }

    single { com.example.mushaf.data.search.remote.SearchApi(get()) }
    single { com.example.mushaf.data.search.remote.SemanticSearchRemoteDataSource(get()) }

    single<MushafRepository> { MushafRepositoryImpl(get(), get(), get(), get(), get()) }
    single<ReaderPreferencesRepository> { ReaderPreferencesRepositoryImpl(get()) }

    single<DownloadableResourceRepository> { FakeDownloadableResourceRepository() }
    
    // Listen Mode - Real API
    single { HttpClient(Android) {
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
    single { com.example.mushaf.data.recitation.remote.QuranApi(get()) }
    single<com.example.mushaf.data.recitation.RecitationDataSource> { 
        com.example.mushaf.data.recitation.remote.RecitationRemoteDataSourceImpl(get()) 
    }
    single<com.example.mushaf.domain.repository.RecitationRepository> { 
        com.example.mushaf.data.repository.RecitationRepositoryImpl(get()) 
    }
}
