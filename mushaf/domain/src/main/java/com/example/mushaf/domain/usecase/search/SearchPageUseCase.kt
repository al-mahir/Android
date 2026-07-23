package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.repository.MushafRepository

class SearchPageUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String): Result<List<Int>> {
        return try {
            Result.success(repository.searchPage(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
