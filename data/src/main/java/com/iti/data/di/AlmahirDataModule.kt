package com.iti.data.di

import com.iti.data.datasource.AlmahirDataSource
import com.iti.data.datasource.AlmahirFakeDataSource
import com.iti.data.repository.AlmahirRepositoryImpl
import com.iti.data.settings.local.AppPreferencesDataStore
import com.iti.data.settings.repository.AppPreferencesRepositoryImpl
import com.iti.data.settings.repository.FakeRecordingsRepository
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.settings.repository.AppPreferencesRepository
import com.iti.domain.settings.repository.RecordingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


val almahirDataModule = module {
    single<AlmahirDataSource> { AlmahirFakeDataSource() }

    single<AlmahirRepository> { AlmahirRepositoryImpl(get()) }

    single { AppPreferencesDataStore(androidContext()) }
    single<AppPreferencesRepository> { AppPreferencesRepositoryImpl(get()) }

    single<RecordingsRepository> { FakeRecordingsRepository() }
}
