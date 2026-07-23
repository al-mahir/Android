package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.TafsirResult
import com.example.mushaf.domain.repository.MushafRepository

class SearchTafsirUseCase(
    private val repository: MushafRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 50, offset: Int = 0): List<TafsirResult> {
        return repository.searchTafsir(query, limit, offset)
    }
}
