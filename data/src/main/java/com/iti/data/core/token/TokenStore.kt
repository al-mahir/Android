package com.iti.data.core.token

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

interface TokenStore {

    val tokens: Flow<TokenPair?>

    suspend fun getTokens(): TokenPair?

    suspend fun save(tokens: TokenPair)

    suspend fun clear()
}

val TokenStore.isLoggedIn: Flow<Boolean> get() = tokens.map { it != null }

suspend fun TokenStore.getAccessToken(): String? = getTokens()?.accessToken

suspend fun TokenStore.getRefreshToken(): String? = getTokens()?.refreshToken
