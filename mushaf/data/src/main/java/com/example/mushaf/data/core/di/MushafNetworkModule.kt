package com.example.mushaf.data.core.di

import android.util.Log
import com.example.mushaf.data.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

const val AI_SERVICE_CLIENT = "aiServiceClient"
const val SEARCH_CLIENT = "searchClient"

private const val TAG = "AiServiceNet"

val mushafNetworkModule = module {
    single(named(AI_SERVICE_CLIENT)) {
        val token = BuildConfig.AI_SERVICE_TOKEN
        val authority = BuildConfig.AI_SERVICE_AUTHORITY
        val secure = BuildConfig.AI_SERVICE_SECURE
        // Must match whichever backend AI_SERVICE_AUTHORITY actually points at (local dev
        // server or the hosted ngrok tunnel) - a hardcoded ngrok Origin sent to a different
        // backend is wrong and would fail Origin validation if that backend ever checks it.
        val origin = "${if (secure) "https" else "http"}://$authority"

        // ── Diagnostic logs ──────────────────────────────────────────────────
        Log.i(TAG, "=== AI Service client init ===")
        Log.i(TAG, "  token blank? ${token.isBlank()} | length=${token.length}")
        Log.i(TAG, "  authority   = $authority")
        Log.i(TAG, "  secure      = $secure")
        // ─────────────────────────────────────────────────────────────────────

        // Header-injection interceptor — logs every header we add so we can
        // confirm they actually reach the wire.
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val req = original.newBuilder()
                .addHeader("ngrok-skip-browser-warning", "true")
                .addHeader("Origin", origin)
                .apply { if (token.isNotBlank()) addHeader("Authorization", "Bearer $token") }
                .build()

            Log.d(TAG, "→ ${req.method} ${req.url}")
            Log.d(TAG, "  Authorization  : ${req.header("Authorization")?.take(20)}...")
            Log.d(TAG, "  Origin         : ${req.header("Origin")}")
            Log.d(TAG, "  ngrok-skip     : ${req.header("ngrok-skip-browser-warning")}")

            val response = chain.proceed(req)
            Log.d(TAG, "← ${response.code} ${response.message}  url=${response.request.url}")
            if (response.code == 403) {
                Log.e(TAG, "!!! 403 — server response headers:")
                response.headers.names().forEach { name ->
                    Log.e(TAG, "  $name: ${response.header(name)}")
                }
            }
            response
        }

        // OkHttp's own verbose HTTP log (shows exact bytes on the wire)
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.v(TAG, message)
        }.apply { level = HttpLoggingInterceptor.Level.HEADERS }

        val aiOkHttpClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)   // long-lived socket; no read timeout
            .pingInterval(120, TimeUnit.SECONDS) // Increased to 120s because local ML inference can block the backend event loop
            .connectTimeout(8, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)          // runs first → adds headers
            .addNetworkInterceptor(loggingInterceptor) // sees final wire headers
            .build()

        HttpClient(OkHttp) {
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false })
            }
            engine {
                preconfigured = aiOkHttpClient
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
