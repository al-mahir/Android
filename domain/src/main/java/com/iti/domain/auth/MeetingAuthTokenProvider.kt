package com.iti.domain.auth

import kotlinx.coroutines.flow.Flow


interface MeetingAuthTokenProvider {
    suspend fun currentToken(): String?


    val sessionChanges: Flow<Unit>

    suspend fun refreshToken(rejectedToken: String? = null): String?

    suspend fun onAuthenticationExpired()
}


