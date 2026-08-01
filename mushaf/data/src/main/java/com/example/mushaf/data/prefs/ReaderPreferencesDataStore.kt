package com.example.mushaf.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.model.ReaderPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mushaf_reader_prefs",
)

 
class ReaderPreferencesDataStore(context: Context) {

    private val dataStore = context.applicationContext.readerDataStore

    val preferences: Flow<ReaderPreferences> = dataStore.data.map { prefs ->
        ReaderPreferences(
            tajweedEnabled = prefs[KEY_TAJWEED] ?: true,
            lastPage = prefs[KEY_LAST_PAGE] ?: MushafConstants.FIRST_PAGE,
            isFirstMushafLaunch = prefs[KEY_FIRST_MUSHAF_LAUNCH] ?: true,
            downloadOverWifiOnly = prefs[KEY_DOWNLOAD_OVER_WIFI_ONLY] ?: false,
        )
    }

    suspend fun setTajweedEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_TAJWEED] = enabled }
    }

    suspend fun setLastPage(page: Int) {
        dataStore.edit { it[KEY_LAST_PAGE] = MushafConstants.clampPage(page) }
    }

    suspend fun setFirstMushafLaunchCompleted() {
        dataStore.edit { it[KEY_FIRST_MUSHAF_LAUNCH] = false }
    }

    suspend fun setDownloadOverWifiOnly(enabled: Boolean) {
        dataStore.edit { it[KEY_DOWNLOAD_OVER_WIFI_ONLY] = enabled }
    }

    private companion object {
        val KEY_TAJWEED = booleanPreferencesKey("tajweed_enabled")
        val KEY_LAST_PAGE = intPreferencesKey("last_page")
        val KEY_FIRST_MUSHAF_LAUNCH = booleanPreferencesKey("first_mushaf_launch")
        val KEY_DOWNLOAD_OVER_WIFI_ONLY = booleanPreferencesKey("download_over_wifi_only")
    }
}
