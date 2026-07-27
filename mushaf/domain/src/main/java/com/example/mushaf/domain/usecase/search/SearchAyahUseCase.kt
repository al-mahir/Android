package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result

class SearchAyahUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String, limit: Int = 50, offset: Int = 0): Result<List<AyahSearchResult>> =
        repository.searchAyah(query, limit, offset)
}
