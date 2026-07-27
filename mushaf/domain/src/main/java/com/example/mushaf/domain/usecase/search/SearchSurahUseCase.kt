package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.Surah
import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result

class SearchSurahUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String): Result<List<Surah>> = repository.searchSurah(query)
}
