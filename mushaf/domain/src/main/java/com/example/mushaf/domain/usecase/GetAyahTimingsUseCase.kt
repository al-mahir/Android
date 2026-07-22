package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.repository.RecitationRepository
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow

/**
 * Use case to fetch word-level audio segment timings for a specific surah and reciter.
 */
class GetAyahTimingsUseCase(
    private val repository: RecitationRepository
) {
    operator fun invoke(reciterId: Int, pageNumber: Int): Flow<Result<List<AyahTiming>>> {
        return repository.getTimingsForPage(reciterId, pageNumber)
    }
}
