package com.example.mushaf.data.repository

import com.example.mushaf.data.prefs.ReaderPreferencesDataStore
import com.iti.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads the last page directly from the Mushaf reader's [ReaderPreferencesDataStore] —
 * the same store that [SaveLastPageUseCase][com.example.mushaf.domain.usecase.SaveLastPageUseCase]
 * writes to whenever the user swipes to a new page.
 */
class ReadingProgressRepositoryImpl(
    private val readerPrefs: ReaderPreferencesDataStore,
) : ReadingProgressRepository {

    override fun observeLastPage(): Flow<Int> =
        readerPrefs.preferences.map { it.lastPage }
}
