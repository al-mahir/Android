package com.iti.data.di

import com.iti.data.datasource.AlmahirDataSource
import com.iti.data.datasource.AlmahirFakeDataSource
import com.iti.data.repository.AlmahirRepositoryImpl
import com.iti.domain.repository.AlmahirRepository
import org.koin.dsl.module


val almahirDataModule = module {
    single<AlmahirDataSource> { AlmahirFakeDataSource() }

    single<AlmahirRepository> { AlmahirRepositoryImpl(get()) }
}
