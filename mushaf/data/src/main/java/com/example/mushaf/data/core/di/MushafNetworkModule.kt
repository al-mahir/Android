package com.example.mushaf.data.core.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

const val AI_SERVICE_CLIENT = "aiServiceClient"
const val SEARCH_CLIENT = "searchClient"

val mushafNetworkModule = module {
    single(named(AI_SERVICE_CLIENT)) {
        HttpClient(OkHttp) {
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false })
            }
            engine {
                config {
                    // No read timeout: the live recitation socket is long-lived and
                    // relies on the ping interval below to detect a dead connection.
                    readTimeout(0, TimeUnit.MILLISECONDS)
                    pingInterval(20, TimeUnit.SECONDS)
                    connectTimeout(4, TimeUnit.SECONDS)
                }
            }
        }
    }

    single(named(SEARCH_CLIENT)) {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000L
                connectTimeoutMillis = 60_000L
                socketTimeoutMillis = 60_000L
            }
        }
    }
}
