package com.iti.domain.auth

/**
 * Bridges the host app's own session/token storage into `:meeting-kit` without this module
 * depending on the host's auth stack. The host implements this by delegating to its own token
 * store; `:meeting-kit` never performs its own token refresh, it only ever reads the freshest
 * value the host currently has.
 */
fun interface MeetingAuthTokenProvider {
    suspend fun currentToken(): String?
}


