package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.repository.MushafRepository

class SearchHizbUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String): Result<List<Hizb>> {
        return try {
            Result.success(repository.searchHizb(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
