package com.example.mushaf.data.repository

import com.example.mushaf.data.prefs.ReaderPreferencesDataStore
import com.example.mushaf.domain.model.ReaderPreferences
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ReaderPreferencesRepositoryImpl(
    private val dataStore: ReaderPreferencesDataStore,
) : ReaderPreferencesRepository {

    override val preferences: Flow<ReaderPreferences> = dataStore.preferences

    override suspend fun setTajweedEnabled(enabled: Boolean) =
        dataStore.setTajweedEnabled(enabled)

    override suspend fun setLastPage(page: Int) = dataStore.setLastPage(page)

    override suspend fun setFirstMushafLaunchCompleted() =
        dataStore.setFirstMushafLaunchCompleted()
}
