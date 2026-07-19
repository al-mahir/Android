package com.example.mushaf.data.di

import com.example.mushaf.data.db.MushafAssetDataSource
import com.example.mushaf.data.db.QuranMetadataDataSource
import com.example.mushaf.data.db.QuranTextDataSource
import com.example.mushaf.data.prefs.ReaderPreferencesDataStore
import com.example.mushaf.data.repository.MushafRepositoryImpl
import com.example.mushaf.data.repository.ReaderPreferencesRepositoryImpl
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val mushafDataModule = module {
    single { MushafAssetDataSource(androidContext()) }
    single { QuranMetadataDataSource(androidContext()) }
    single { QuranTextDataSource(androidContext()) }
    single { ReaderPreferencesDataStore(androidContext()) }

    single<MushafRepository> { MushafRepositoryImpl(get(), get(), get()) }
    single<ReaderPreferencesRepository> { ReaderPreferencesRepositoryImpl(get()) }
}
