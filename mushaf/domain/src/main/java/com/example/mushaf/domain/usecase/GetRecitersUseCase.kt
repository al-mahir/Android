package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.domain.repository.RecitationRepository
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow

/**
 * Use case to fetch the catalog of available reciters.
 */
class GetRecitersUseCase(
    private val repository: RecitationRepository
) {
    operator fun invoke(): Flow<Result<List<Reciter>>> {
        return repository.getReciters()
    }
}
