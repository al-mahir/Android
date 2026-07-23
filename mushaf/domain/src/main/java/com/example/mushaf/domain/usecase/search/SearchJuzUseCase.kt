package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.repository.MushafRepository

class SearchJuzUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String): Result<List<Juz>> {
        return try {
            Result.success(repository.searchJuz(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
