package com.iti.data.core.token

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authTokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_tokens",
)


class TokenStorage(context: Context) : TokenStore {

    private val dataStore = context.applicationContext.authTokenDataStore

    override val tokens: Flow<TokenPair?> = dataStore.data.map { prefs ->
        val accessToken = prefs[KEY_ACCESS_TOKEN]
        val refreshToken = prefs[KEY_REFRESH_TOKEN]
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            null
        } else {
            TokenPair(accessToken, refreshToken)
        }
    }

    override suspend fun getTokens(): TokenPair? = tokens.first()

    override suspend fun save(tokens: TokenPair) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = tokens.accessToken
            prefs[KEY_REFRESH_TOKEN] = tokens.refreshToken
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
        }
    }

    private companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
