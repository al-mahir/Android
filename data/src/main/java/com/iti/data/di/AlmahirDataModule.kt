package com.iti.data.di

import com.iti.data.datasource.home.FakeHomeDataSource
import com.iti.data.datasource.home.HomeDataSource
import com.iti.data.repository.AlmahirRepositoryImpl
import com.iti.domain.repository.AlmahirRepository
import org.koin.dsl.module

/**
 * Data-layer bindings for the general app content.
 *
 * [HomeDataSource] is bound to [FakeHomeDataSource] while the backend is in progress —
 * swapping in the Ktor-backed source is a one-line change on that single binding.
 */
val almahirDataModule = module {
    single<HomeDataSource> { FakeHomeDataSource() }

    single<AlmahirRepository> { AlmahirRepositoryImpl(get()) }
}
