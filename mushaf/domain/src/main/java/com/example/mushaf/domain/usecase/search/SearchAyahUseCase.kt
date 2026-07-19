package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.repository.MushafRepository

class SearchAyahUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String): Result<List<AyahSearchResult>> {
        return try {
            Result.success(repository.searchAyah(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
