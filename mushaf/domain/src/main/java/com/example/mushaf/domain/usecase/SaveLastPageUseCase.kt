package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.repository.ReaderPreferencesRepository

class SaveLastPageUseCase(
    private val repository: ReaderPreferencesRepository,
) {
    suspend operator fun invoke(page: Int) =
        repository.setLastPage(MushafConstants.clampPage(page))
}
