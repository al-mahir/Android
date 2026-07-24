package com.iti.data.repository

import com.example.mushaf.data.prefs.ReaderPreferencesDataStore
import com.iti.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads the last page directly from the Mushaf reader's [ReaderPreferencesDataStore] —
 * the same store that [SaveLastPageUseCase][com.example.mushaf.domain.usecase.SaveLastPageUseCase]
 * writes to whenever the user swipes to a new page.
 *
 * This bridges the two module families without introducing a new module dependency:
 * - [ReaderPreferencesDataStore] is already a Koin `single` in `mushafDataModule`, so it is
 *   available in the same Koin graph as [AlmahirRepositoryImpl].
 * - [ReadingProgressRepository] lives in `:domain`, which `:data` already depends on.
 */
class ReadingProgressRepositoryImpl(
    private val readerPrefs: ReaderPreferencesDataStore,
) : ReadingProgressRepository {

    override fun observeLastPage(): Flow<Int> =
        readerPrefs.preferences.map { it.lastPage }
}
