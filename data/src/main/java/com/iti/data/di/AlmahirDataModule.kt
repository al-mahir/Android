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
    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
    includes(networkModule)

    single { AppPreferencesDataStore(androidContext()) }

    // ── Data sources ──────────────────────────────────────────────────────────
    single<AlmahirDataSource> { AlmahirFakeDataSource(get()) }
    single<SheikhDataSource> { SheikhRemoteDataSource(httpClient = get(AlmahirClient)) }
    single<CircleDataSource> { FakeCircleDataSource() }

    single { AlmahirDatabase.create(androidContext()) }
    single { get<AlmahirDatabase>().recitationSessionDao() }
    single { get<AlmahirDatabase>().bookmarkDao() }
    single { get<AlmahirDatabase>().meetingStatusDao() }
    single<AlmahirLocalDataSource> { AlmahirLocalDataSourceImpl(get()) }
    single { AlmahirRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
    single<AlmahirRepository> { get<AlmahirRepositoryImpl>() }
    single<SheikhRepository> { get<AlmahirRepositoryImpl>() }
    single<CircleRepository> { get<AlmahirRepositoryImpl>() }
    single<RecitationSessionRepository> { get<AlmahirRepositoryImpl>() }

    single { SettingsRepositoryImpl(get()) }
    single<AppPreferencesRepository> { get<SettingsRepositoryImpl>() }
    single<RecordingsRepository> { get<SettingsRepositoryImpl>() }
}
