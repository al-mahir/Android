package com.iti.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Provides the user's actual reading position as persisted by the Mushaf reader.
 *
 * Implemented in `:data` by reading from the same DataStore that
 * [SaveLastPageUseCase][com.example.mushaf.domain.usecase.SaveLastPageUseCase] writes to,
 * so the home-screen card always reflects what the user last read.
 */
interface ReadingProgressRepository {
    /** Emits the 1-based page number of the last page the user opened (default: 1). */
    fun observeLastPage(): Flow<Int>
}
