package com.example.mushaf.data.recite.remote

import com.example.mushaf.data.BuildConfig



















 
data class AiServiceConfig(
    val authority: String = DEFAULT_AUTHORITY,
     
    val secure: Boolean = DEFAULT_SECURE,

    val token: String = DEFAULT_TOKEN,
) {
    val httpBaseUrl: String get() = "${if (secure) "https" else "http"}://$authority"

    val webSocketBaseUrl: String get() = "${if (secure) "wss" else "ws"}://$authority"

     
    val sessionUrl: String get() = "$webSocketBaseUrl$SESSION_PATH"

    companion object {
        



 
        val DEFAULT_AUTHORITY: String = BuildConfig.AI_SERVICE_AUTHORITY
        val DEFAULT_SECURE: Boolean = BuildConfig.AI_SERVICE_SECURE
        val DEFAULT_TOKEN: String = BuildConfig.AI_SERVICE_TOKEN

        const val SESSION_PATH = "/ws/session"
    }
}
