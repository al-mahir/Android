package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.TafsirBook
import com.example.mushaf.domain.repository.MushafRepository
import kotlinx.coroutines.flow.Flow

class ObserveAvailableTafsirBooksUseCase(
    private val repository: MushafRepository,
) {
    operator fun invoke(): Flow<List<TafsirBook>> {
        return repository.observeAvailableTafsirBooks()
    }
}
