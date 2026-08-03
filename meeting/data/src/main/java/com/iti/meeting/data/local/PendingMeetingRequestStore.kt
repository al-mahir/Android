package com.iti.meeting.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iti.meeting.domain.model.PendingMeetingRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.pendingMeetingRequestDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "meeting_pending_request_prefs",
)

class PendingMeetingRequestStore(context: Context) {

    private val dataStore = context.applicationContext.pendingMeetingRequestDataStore

    val pendingRequest: Flow<PendingMeetingRequest?> = dataStore.data.map { prefs ->
        val requestId = prefs[KEY_REQUEST_ID] ?: return@map null
        val sheikhId = prefs[KEY_SHEIKH_ID] ?: return@map null
        val expiresAt = prefs[KEY_EXPIRES_AT] ?: return@map null
        PendingMeetingRequest(
            requestId = requestId,
            sheikhId = sheikhId,
            sheikhName = prefs[KEY_SHEIKH_NAME],
            expiresAt = expiresAt,
        )
    }

    suspend fun get(): PendingMeetingRequest? = pendingRequest.first()

    suspend fun save(request: PendingMeetingRequest) {
        dataStore.edit { prefs ->
            prefs[KEY_REQUEST_ID] = request.requestId
            prefs[KEY_SHEIKH_ID] = request.sheikhId
            prefs[KEY_EXPIRES_AT] = request.expiresAt
            val sheikhName = request.sheikhName
            if (sheikhName != null) {
                prefs[KEY_SHEIKH_NAME] = sheikhName
            } else {
                prefs.remove(KEY_SHEIKH_NAME)
            }
        }
    }

    /** Only clears the stored record if it matches [requestId] — avoids racily wiping a newer
     * request out from under a stale event for an older one. */
    suspend fun clearIfMatches(requestId: String) {
        dataStore.edit { prefs ->
            if (prefs[KEY_REQUEST_ID] == requestId) {
                prefs.clear()
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_REQUEST_ID = stringPreferencesKey("request_id")
        val KEY_SHEIKH_ID = stringPreferencesKey("sheikh_id")
        val KEY_SHEIKH_NAME = stringPreferencesKey("sheikh_name")
        val KEY_EXPIRES_AT = stringPreferencesKey("expires_at")
    }
}
