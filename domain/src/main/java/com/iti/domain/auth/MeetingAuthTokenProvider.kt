package com.iti.domain.auth

/**
 * Bridges the host app's own session/token storage into `:meeting-kit` without this module
 * depending on the host's auth stack. The host implements this by delegating to its own token
 * store; `:meeting-kit`'s own HTTP client has no refresh logic of its own — [refreshToken] and
 * [onAuthenticationExpired] are how it asks the host to do that on its behalf when a request
 * comes back 401/403.
 */
interface MeetingAuthTokenProvider {
    suspend fun currentToken(): String?

    /** Asks the host to refresh the access token right now (not tied to any particular failed
     * request) and returns the new token, or `null` if the refresh itself failed. */
    suspend fun refreshToken(): String?

    /** Called once refreshing has been tried and a request still comes back unauthorized — the
     * host should clear its session so the user is routed back to the login screen. */
    suspend fun onAuthenticationExpired()
}


