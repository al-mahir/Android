package com.iti.data.di

import com.iti.data.core.di.AlmahirClient
import com.iti.data.core.di.networkModule
import com.iti.data.datasource.AlmahirDataSource
import com.iti.data.datasource.AlmahirFakeDataSource
import com.iti.data.datasource.circle.CircleDataSource
import com.iti.data.datasource.circle.FakeCircleDataSource
import com.iti.data.datasource.sheikh.SheikhDataSource
import com.iti.data.datasource.sheikh.SheikhRemoteDataSource
import com.iti.data.local.AlmahirDatabase
import com.iti.data.repository.AlmahirRepositoryImpl
import com.iti.data.repository.CircleRepositoryImpl
import com.iti.data.repository.RecitationSessionRepositoryImpl
import com.iti.data.repository.SheikhRepositoryImpl
import com.iti.data.settings.local.AppPreferencesDataStore
import com.iti.data.settings.repository.AppPreferencesRepositoryImpl
import com.iti.data.settings.repository.FakeRecordingsRepository
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.repository.CircleRepository
import com.iti.domain.repository.ReadingProgressRepository
import com.iti.domain.repository.RecitationSessionRepository
import com.iti.domain.repository.SheikhRepository
import com.iti.domain.settings.repository.AppPreferencesRepository
import com.iti.domain.settings.repository.RecordingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


val almahirDataModule = module {
    // ── Network (shared HTTP client, token store, JSON) ──────────────────────
    includes(networkModule)

    // ── Sheikh — real API ─────────────────────────────────────────────────────
    single<SheikhDataSource> { SheikhRemoteDataSource(httpClient = get(AlmahirClient)) }
    single<SheikhRepository> { SheikhRepositoryImpl(dataSource = get()) }

    // ── Circle — fake until backend is ready ─────────────────────────────────
    single<CircleDataSource> { FakeCircleDataSource() }
    single<CircleRepository> { CircleRepositoryImpl(dataSource = get()) }

    // ── Monolithic legacy repo (user, reading, subscription, legal) ───────────
    // Still backed by AlmahirFakeDataSource; sheikh/circle methods on it
    // are now dead code but cause no harm. Will be cleaned up when all domains migrate.
    single<AlmahirDataSource> { AlmahirFakeDataSource() }
    single<AlmahirRepository> { AlmahirRepositoryImpl(get()) }
    single<ReadingProgressRepository> { com.iti.data.repository.ReadingProgressRepositoryImpl(get()) }

    // ── App preferences ───────────────────────────────────────────────────────
    single { AppPreferencesDataStore(androidContext()) }
    single<AppPreferencesRepository> { AppPreferencesRepositoryImpl(get()) }

    // ── Recordings ────────────────────────────────────────────────────────────
    single<RecordingsRepository> { FakeRecordingsRepository() }

    // ── Room ──────────────────────────────────────────────────────────────────
    single { AlmahirDatabase.create(androidContext()) }
    single { get<AlmahirDatabase>().recitationSessionDao() }
    single<RecitationSessionRepository> { RecitationSessionRepositoryImpl(get()) }
}
