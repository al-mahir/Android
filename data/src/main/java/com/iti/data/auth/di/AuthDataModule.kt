package com.iti.data.auth.di

import com.iti.data.auth.local.TokenStorage
import com.iti.data.auth.remote.AuthRemoteDataSource
import com.iti.data.auth.repository.AuthRepositoryImpl
import com.iti.domain.auth.repository.AuthRepository
import org.koin.dsl.module

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

import io.ktor.client.plugins.logging.Logger
import android.util.Log

val authDataModule = module {
    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("KtorClient", message)
                    }
                }
                level = LogLevel.ALL
            }
            defaultRequest {
                url("https://virtserver.swaggerhub.com/iti-ff4/AuthN-AuthZ-API/1.4.0/")
                contentType(ContentType.Application.Json)
            }
        }
    }
    single { TokenStorage(get()) }
    single { AuthRemoteDataSource(get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
}
