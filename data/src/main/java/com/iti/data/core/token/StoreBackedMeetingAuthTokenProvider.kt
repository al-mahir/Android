package com.iti.data.core.token

import com.iti.domain.auth.MeetingAuthTokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map


class StoreBackedMeetingAuthTokenProvider(
    private val tokenStore: TokenStore,
    private val refresher: TokenRefresher,
) : MeetingAuthTokenProvider {

    override suspend fun currentToken(): String? {
        val accessToken = tokenStore.getTokens()?.accessToken ?: return null
        if (!accessToken.isAccessTokenExpiredOrNearExpiry()) return accessToken
        return refresher.refresh(rejectedAccessToken = accessToken)?.accessToken ?: accessToken
    }

    override suspend fun refreshToken(rejectedToken: String?): String? =
        refresher.refresh(rejectedAccessToken = rejectedToken)?.accessToken

    override suspend fun onAuthenticationExpired() = tokenStore.clear()

    override val sessionChanges: Flow<Unit> = tokenStore.tokens
        .map { it?.accessToken }
        .distinctUntilChanged()
        .drop(1)
        .map { }
}
