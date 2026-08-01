package com.iti.data.di

import com.iti.data.core.di.AlmahirClient
import com.iti.data.core.di.networkModule
import com.iti.data.datasource.AlmahirDataSource
import com.iti.data.datasource.AlmahirFakeDataSource
import com.iti.data.datasource.AlmahirLocalDataSource
import com.iti.data.datasource.AlmahirLocalDataSourceImpl
import com.iti.data.datasource.circle.CircleDataSource
import com.iti.data.datasource.circle.FakeCircleDataSource
import com.iti.data.datasource.sheikh.SheikhDataSource
import com.iti.data.datasource.sheikh.SheikhRemoteDataSource
import com.iti.data.local.AlmahirDatabase
import com.iti.data.repository.AlmahirRepositoryImpl
import com.iti.data.settings.local.AppPreferencesDataStore
import com.iti.data.settings.repository.SettingsRepositoryImpl
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.repository.CircleRepository
import com.iti.domain.repository.RecitationSessionRepository
import com.iti.domain.repository.SheikhRepository
import com.iti.domain.settings.repository.AppPreferencesRepository
import com.iti.domain.settings.repository.RecordingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

import com.iti.data.connectivity.AndroidConnectivityObserver
import com.iti.domain.connectivity.ConnectivityObserver

val almahirDataModule = module {
    // ── Connectivity ──────────────────────────────────────────────────────────
    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
    // ── Network (shared HTTP client, token store, JSON) ──────────────────────
    includes(networkModule)

    // ── Data sources ──────────────────────────────────────────────────────────
    single<AlmahirDataSource> { AlmahirFakeDataSource() }
    single<SheikhDataSource> { SheikhRemoteDataSource(httpClient = get(AlmahirClient)) }
    single<CircleDataSource> { FakeCircleDataSource() }

    // ── Almahir bucket: user/subscription/legal-doc/account, sheikh, circle,
    //    and saved recitation sessions all live on one merged repository ──────
    single { AlmahirDatabase.create(androidContext()) }
    single { get<AlmahirDatabase>().recitationSessionDao() }
    single { get<AlmahirDatabase>().bookmarkDao() }
    single<AlmahirLocalDataSource> { AlmahirLocalDataSourceImpl(get()) }
    single { AlmahirRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<AlmahirRepository> { get<AlmahirRepositoryImpl>() }
    single<SheikhRepository> { get<AlmahirRepositoryImpl>() }
    single<CircleRepository> { get<AlmahirRepositoryImpl>() }
    single<RecitationSessionRepository> { get<AlmahirRepositoryImpl>() }

    // ── App preferences + recordings (kept separate: Context-backed DataStore
    //    is eagerly constructed, and :data's unit tests have no mocking library,
    //    so merging it into the DAO-based repo above would force every test of
    //    that class to build a real Context) ──────────────────────────────────
    single { AppPreferencesDataStore(androidContext()) }
    single { SettingsRepositoryImpl(get()) }
    single<AppPreferencesRepository> { get<SettingsRepositoryImpl>() }
    single<RecordingsRepository> { get<SettingsRepositoryImpl>() }
}
