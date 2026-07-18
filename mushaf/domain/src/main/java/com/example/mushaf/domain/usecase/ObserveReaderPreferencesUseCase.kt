package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.ReaderPreferences
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveReaderPreferencesUseCase(
    private val repository: ReaderPreferencesRepository,
) {
    operator fun invoke(): Flow<ReaderPreferences> = repository.preferences
}
