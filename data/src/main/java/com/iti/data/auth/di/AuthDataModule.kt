package com.iti.data.auth.di

import com.iti.data.auth.remote.AuthRemoteDataSource
import com.iti.data.auth.repository.AuthRepositoryImpl
import com.iti.data.core.di.AlmahirClient
import com.iti.data.core.di.networkModule
import com.iti.domain.auth.repository.AuthRepository
import org.koin.dsl.module

val authDataModule = module {
    includes(networkModule)

    single { AuthRemoteDataSource(client = get(AlmahirClient), json = get(AlmahirClient)) }
    single<AuthRepository> { AuthRepositoryImpl(remoteDataSource = get(), tokenStore = get(), appPreferencesDataStore = get()) }
}
