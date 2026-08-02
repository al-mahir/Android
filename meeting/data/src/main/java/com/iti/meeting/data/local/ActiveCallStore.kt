package com.iti.meeting.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iti.meeting.domain.model.ActiveCallRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.activeCallDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "meeting_active_call_prefs",
)

/** Mirrors [PendingMeetingRequestStore] exactly — the one piece of call state that survives real
 * process death, since `CallSessionController`'s in-memory state does not. */
class ActiveCallStore(context: Context) {

    private val dataStore = context.applicationContext.activeCallDataStore

    val activeCall: Flow<ActiveCallRecord?> = dataStore.data.map { prefs ->
        val requestId = prefs[KEY_REQUEST_ID] ?: return@map null
        val channelName = prefs[KEY_CHANNEL_NAME] ?: return@map null
        val userAccount = prefs[KEY_USER_ACCOUNT] ?: return@map null
        ActiveCallRecord(
            requestId = requestId,
            channelName = channelName,
            userAccount = userAccount,
            remoteDisplayName = prefs[KEY_REMOTE_DISPLAY_NAME],
        )
    }

    suspend fun get(): ActiveCallRecord? = activeCall.first()

    suspend fun save(record: ActiveCallRecord) {
        dataStore.edit { prefs ->
            prefs[KEY_REQUEST_ID] = record.requestId
            prefs[KEY_CHANNEL_NAME] = record.channelName
            prefs[KEY_USER_ACCOUNT] = record.userAccount
            val remoteDisplayName = record.remoteDisplayName
            if (remoteDisplayName != null) {
                prefs[KEY_REMOTE_DISPLAY_NAME] = remoteDisplayName
            } else {
                prefs.remove(KEY_REMOTE_DISPLAY_NAME)
            }
        }
    }

    /** Only clears the stored record if it matches [requestId] — avoids racily wiping a newer
     * call out from under a stale terminal event for an older one. */
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
        val KEY_CHANNEL_NAME = stringPreferencesKey("channel_name")
        val KEY_USER_ACCOUNT = stringPreferencesKey("user_account")
        val KEY_REMOTE_DISPLAY_NAME = stringPreferencesKey("remote_display_name")
    }
}
