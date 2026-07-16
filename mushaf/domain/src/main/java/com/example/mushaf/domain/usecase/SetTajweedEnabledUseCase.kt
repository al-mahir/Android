package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.repository.ReaderPreferencesRepository

class SetTajweedEnabledUseCase(
    private val repository: ReaderPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setTajweedEnabled(enabled)
}
