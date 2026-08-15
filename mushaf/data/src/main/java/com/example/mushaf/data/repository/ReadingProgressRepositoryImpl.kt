package com.example.mushaf.data.repository

import com.example.mushaf.data.prefs.ReaderPreferencesDataStore
import com.iti.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class ReadingProgressRepositoryImpl(
    private val readerPrefs: ReaderPreferencesDataStore,
) : ReadingProgressRepository {

    override fun observeLastPage(): Flow<Int?> =
        readerPrefs.preferences.map { 
            if (it.isFirstMushafLaunch) null else it.lastPage 
        }
}
