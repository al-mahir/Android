package com.example.mushaf.data.recite.remote

import com.example.mushaf.data.BuildConfig



















 
data class AiServiceConfig(
    val authority: String = DEFAULT_AUTHORITY,
     
    val secure: Boolean = false,
) {
    val httpBaseUrl: String get() = "${if (secure) "https" else "http"}://$authority"

    val webSocketBaseUrl: String get() = "${if (secure) "wss" else "ws"}://$authority"

     
    val sessionUrl: String get() = "$webSocketBaseUrl$SESSION_PATH"

    companion object {
        



 
        val DEFAULT_AUTHORITY: String = BuildConfig.AI_SERVICE_AUTHORITY

        const val SESSION_PATH = "/ws/session"
    }
}
