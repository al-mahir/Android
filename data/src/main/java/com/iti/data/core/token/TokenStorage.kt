package com.iti.data.core.token

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

class TokenStorage(context: Context) : TokenStore {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_tokens_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override val tokens: Flow<TokenPair?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ACCESS_TOKEN || key == KEY_REFRESH_TOKEN) {
                trySend(readTokens())
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.onStart {
        emit(readTokens())
    }

    private fun readTokens(): TokenPair? {
        val accessToken = sharedPrefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = sharedPrefs.getString(KEY_REFRESH_TOKEN, null)
        return if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            null
        } else {
            TokenPair(accessToken, refreshToken)
        }
    }

    override suspend fun getTokens(): TokenPair? = readTokens()

    override suspend fun save(tokens: TokenPair) {
        sharedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            .putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
            .apply()
    }

    override suspend fun clear() {
        sharedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
