package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.repository.ReaderPreferencesRepository

class SetFirstMushafLaunchCompletedUseCase(
    private val repository: ReaderPreferencesRepository,
) {
    suspend operator fun invoke() = repository.setFirstMushafLaunchCompleted()
}
