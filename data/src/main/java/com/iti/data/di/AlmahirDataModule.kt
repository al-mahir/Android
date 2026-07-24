package com.iti.data.di

import com.iti.data.datasource.AlmahirDataSource
import com.iti.data.local.AlmahirDatabase
import com.iti.data.repository.RecitationSessionRepositoryImpl
import com.iti.data.datasource.AlmahirFakeDataSource
import com.iti.data.repository.AlmahirRepositoryImpl
import com.iti.data.settings.local.AppPreferencesDataStore
import com.iti.data.settings.repository.AppPreferencesRepositoryImpl
import com.iti.data.settings.repository.FakeRecordingsRepository
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.repository.ReadingProgressRepository
import com.iti.domain.repository.RecitationSessionRepository
import com.iti.domain.settings.repository.AppPreferencesRepository
import com.iti.domain.settings.repository.RecordingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


val almahirDataModule = module {
    single<AlmahirDataSource> { AlmahirFakeDataSource() }

    single<AlmahirRepository> { AlmahirRepositoryImpl(get()) }
    single<ReadingProgressRepository> { com.iti.data.repository.ReadingProgressRepositoryImpl(get()) }

    single { AppPreferencesDataStore(androidContext()) }
    single<AppPreferencesRepository> { AppPreferencesRepositoryImpl(get()) }

    single<RecordingsRepository> { FakeRecordingsRepository() }

    single { AlmahirDatabase.create(androidContext()) }
    single { get<AlmahirDatabase>().recitationSessionDao() }
    single<RecitationSessionRepository> { RecitationSessionRepositoryImpl(get()) }
}
