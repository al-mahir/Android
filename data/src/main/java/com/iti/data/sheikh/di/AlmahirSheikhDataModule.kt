package com.iti.data.sheikh.di

import com.iti.data.core.di.SheikhAlmahirClient
import com.iti.data.core.di.sheikhNetworkModule
import com.iti.data.sheikh.local.AlmahirSheikhLocalDataSource
import com.iti.data.sheikh.local.InMemoryAlmahirSheikhLocalDataSource
import com.iti.data.sheikh.remote.AlmahirSheikhRemoteDataSource
import com.iti.data.sheikh.remote.AlmahirSheikhRemoteDataSourceImpl
import com.iti.data.sheikh.repository.AlmahirSheikhRepositoryImpl
import com.iti.domain.repository.AlmahirSheikhRepository
import org.koin.dsl.module

val almahirSheikhDataModule = module {
    includes(sheikhNetworkModule)

    single<AlmahirSheikhLocalDataSource> { InMemoryAlmahirSheikhLocalDataSource() }
    single<AlmahirSheikhRemoteDataSource> { AlmahirSheikhRemoteDataSourceImpl(httpClient = get(SheikhAlmahirClient)) }
    single<AlmahirSheikhRepository> {
        AlmahirSheikhRepositoryImpl(local = get(), remote = get(), tokenStore = get())
    }
}
