package com.iti.data.core.di

import com.iti.data.core.network.AlmahirApi
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
val SheikhAlmahirClient = named("sheikh-almahir-http-client")

val networkModule = module {
    single<Json>(AlmahirClient) { AlmahirJson }
    single<TokenStore> { TokenStorage(androidContext()) }
    single<HttpClient>(AlmahirClient) {
        createAlmahirHttpClient(tokenStore = get(), json = get(AlmahirClient))
    }
}

/**
 * Sheikh-flavored client: same [createAlmahirHttpClient] config as [networkModule], but its
 * automatic token refresh calls the sheikh refresh endpoint instead of the student one — the
 * two account families are on different backend routes.
 */
val sheikhNetworkModule = module {
    includes(networkModule)

    single<HttpClient>(SheikhAlmahirClient) {
        createAlmahirHttpClient(
            tokenStore = get(),
            json = get(AlmahirClient),
            refreshEndpoint = AlmahirApi.Auth.Sheikh.REFRESH,
            isPublicEndpoint = AlmahirApi.Auth.Sheikh::isPublic,
        )
    }
}
