package com.iti.data.sheikh.auth.di

import com.iti.data.core.di.AlmahirClient
import com.iti.data.core.di.SheikhAlmahirClient
import com.iti.data.core.di.sheikhNetworkModule
import com.iti.data.sheikh.auth.remote.SheikhAuthRemoteDataSource
import com.iti.data.sheikh.auth.repository.SheikhAuthRepositoryImpl
import com.iti.domain.auth.repository.AuthRepository
import org.koin.dsl.module

val sheikhAuthDataModule = module {
    includes(sheikhNetworkModule)

    single {
        SheikhAuthRemoteDataSource(client = get(SheikhAlmahirClient), json = get(AlmahirClient))
    }
    single<AuthRepository> {
        SheikhAuthRepositoryImpl(remoteDataSource = get(), tokenStore = get(), appPreferencesDataStore = get())
    }
}
