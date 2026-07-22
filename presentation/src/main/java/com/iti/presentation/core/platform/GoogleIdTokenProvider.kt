package com.iti.presentation.core.platform

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed interface GoogleIdTokenResult {

    data class Success(val idToken: String) : GoogleIdTokenResult

    data object Cancelled : GoogleIdTokenResult

    data object Unavailable : GoogleIdTokenResult

    data class Failed(val cause: Throwable) : GoogleIdTokenResult
}


interface GoogleIdTokenProvider {

    val isConfigured: Boolean

    suspend fun requestIdToken(context: Context): GoogleIdTokenResult
}

class CredentialManagerGoogleIdTokenProvider(
    private val webClientId: String,
) : GoogleIdTokenProvider {

    override val isConfigured: Boolean = webClientId.isNotBlank()

    override suspend fun requestIdToken(context: Context): GoogleIdTokenResult {
        if (!isConfigured) return GoogleIdTokenResult.Unavailable
        val activity = context.findActivity() ?: return GoogleIdTokenResult.Unavailable

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        return try {
            val credential = CredentialManager.create(activity)
                .getCredential(activity, request)
                .credential

            when {
                credential is CustomCredential && credential.type in GOOGLE_CREDENTIAL_TYPES ->
                    GoogleIdTokenResult.Success(
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    )

                else -> GoogleIdTokenResult.Failed(
                    IllegalStateException("Unexpected credential type: ${credential.type}")
                )
            }
        } catch (cancellation: GetCredentialCancellationException) {
            GoogleIdTokenResult.Cancelled
        } catch (noCredential: NoCredentialException) {
            GoogleIdTokenResult.Unavailable
        } catch (failure: GetCredentialException) {
            GoogleIdTokenResult.Failed(failure)
        }
    }

    private companion object {
        val GOOGLE_CREDENTIAL_TYPES = setOf(
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
        )
    }
}
