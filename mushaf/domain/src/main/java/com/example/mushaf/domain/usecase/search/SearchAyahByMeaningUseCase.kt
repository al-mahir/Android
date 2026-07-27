package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result

class SearchAyahByMeaningUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(
        query: String,
        mode: String = "hybrid",
        hyde: Boolean = true,
        limit: Int = 20
    ): Result<List<AyahSearchResult>> = repository.searchAyahByMeaning(query, mode, hyde, limit)
}
