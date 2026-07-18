package com.iti.data.auth.di

import com.iti.data.auth.local.TokenStorage
import com.iti.data.auth.remote.AuthRemoteDataSource
import com.iti.data.auth.repository.AuthRepositoryImpl
import com.iti.domain.auth.repository.AuthRepository
import org.koin.dsl.module

val authDataModule = module {
    single { TokenStorage(get()) }
    single { AuthRemoteDataSource() } // Assuming it will take Ktor client later
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
}
