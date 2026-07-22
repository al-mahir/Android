package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.repository.MushafRepository

class SearchAyahUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String, limit: Int = 50, offset: Int = 0): Result<List<AyahSearchResult>> {
        return try {
            Result.success(repository.searchAyah(query, limit, offset))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
