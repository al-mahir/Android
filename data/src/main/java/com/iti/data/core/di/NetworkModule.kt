package com.iti.data.core.di

import com.iti.data.core.network.AlmahirJson
import com.iti.data.core.network.createAlmahirHttpClient
import com.iti.data.core.token.TokenStorage
import com.iti.data.core.token.TokenStore
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val AlmahirClient = named("almahir-http-client")

val networkModule = module {
    single<Json>(AlmahirClient) { AlmahirJson }
    single<TokenStore> { TokenStorage(androidContext()) }
    single<HttpClient>(AlmahirClient) {
        createAlmahirHttpClient(tokenStore = get(), json = get(AlmahirClient))
    }
}
