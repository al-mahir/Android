package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.Surah
import com.example.mushaf.domain.repository.MushafRepository

class SearchSurahUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String): Result<List<Surah>> {
        return try {
            Result.success(repository.searchSurah(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
