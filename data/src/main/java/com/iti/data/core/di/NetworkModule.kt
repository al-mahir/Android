package com.iti.data.core.di

import com.iti.data.core.network.AlmahirJson
import com.iti.data.core.network.AuthRoutes
import com.iti.data.core.network.createAlmahirHttpClient
import com.iti.data.core.token.StoreBackedMeetingAuthTokenProvider
import com.iti.data.core.token.TokenRefresher
import com.iti.data.core.token.TokenStorage
import com.iti.data.core.token.TokenStore
import com.iti.domain.auth.MeetingAuthTokenProvider
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val AlmahirClient = named("almahir-http-client")
val SheikhAlmahirClient = named("sheikh-almahir-http-client")


private val baseNetworkModule = module {
    single<Json>(AlmahirClient) { AlmahirJson }
    single<TokenStore> { TokenStorage(androidContext()) }

    single {
        TokenRefresher(tokenStore = get(), refreshEndpoint = get<AuthRoutes>().refreshEndpoint)
    }
    single<MeetingAuthTokenProvider> {
        StoreBackedMeetingAuthTokenProvider(tokenStore = get(), refresher = get())
    }

    single<HttpClient>(AlmahirClient) {
        val routes = get<AuthRoutes>()
        createAlmahirHttpClient(
            tokenStore = get(),
            json = get(AlmahirClient),
            refreshEndpoint = routes.refreshEndpoint,
            isPublicEndpoint = routes.isPublicEndpoint,
        )
    }
}

val networkModule = module {
    includes(baseNetworkModule)

    single { AuthRoutes.Student }
}


val sheikhNetworkModule = module {
    includes(baseNetworkModule)

    single { AuthRoutes.Sheikh }
    single<HttpClient>(SheikhAlmahirClient) { get(AlmahirClient) }
}
