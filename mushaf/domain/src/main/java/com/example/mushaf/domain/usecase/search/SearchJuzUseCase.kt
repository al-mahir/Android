package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result

class SearchJuzUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String): Result<List<Juz>> = repository.searchJuz(query)
}
