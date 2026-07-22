package com.example.mushaf.data.recite.remote

import com.example.mushaf.data.BuildConfig

/**
 * Where the Al-Mahir AI service lives.
 *
 * Held as one host:port and derived into both schemes, so HTTP and WebSocket can never drift
 * apart. Configurable rather than compiled in (MOBILE_INTEGRATION.md §15) — a gateway may later
 * sit in front and add a path prefix.
 *
 * Reaching a laptop from a device, per MOBILE_INTEGRATION.md §2:
 *
 * | Client | `authority` |
 * |---|---|
 * | Android emulator | `10.0.2.2:8100` — the alias for the host loopback |
 * | Genymotion | `10.0.3.2:8100` |
 * | Physical device, `adb reverse tcp:8100 tcp:8100` | `localhost:8100` — no firewall rule needed |
 * | Physical device over Wi-Fi | the laptop's LAN IP, which DHCP will change |
 *
 * `localhost` from an emulator means the emulator itself — the single most common cause of a
 * connection that times out for no visible reason.
 */
data class AiServiceConfig(
    val authority: String = DEFAULT_AUTHORITY,
    /** False only while the local dev server has no TLS. Debug builds permit cleartext hosts. */
    val secure: Boolean = false,
) {
    val httpBaseUrl: String get() = "${if (secure) "https" else "http"}://$authority"

    val webSocketBaseUrl: String get() = "${if (secure) "wss" else "ws"}://$authority"

    /** The live recitation endpoint. */
    val sessionUrl: String get() = "$webSocketBaseUrl$SESSION_PATH"

    companion object {
        /**
         * Set at build time from `almahir.aiService` in `local.properties`, falling back to the
         * emulator alias. Configurable rather than compiled in, and read from a git-ignored file
         * so a machine's LAN address never lands in version control.
         */
        val DEFAULT_AUTHORITY: String = BuildConfig.AI_SERVICE_AUTHORITY

        const val SESSION_PATH = "/ws/session"
    }
}
