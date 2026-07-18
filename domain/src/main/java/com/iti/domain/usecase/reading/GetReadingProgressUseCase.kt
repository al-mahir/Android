package com.iti.domain.usecase.reading

import com.iti.domain.model.ReadingProgress
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow

class GetReadingProgressUseCase(
    private val repository: AlmahirRepository,
) {
    operator fun invoke(): Flow<ReadingProgress?> = repository.observeReadingProgress()
}
