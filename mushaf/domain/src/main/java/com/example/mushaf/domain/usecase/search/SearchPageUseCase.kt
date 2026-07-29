package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result

class SearchPageUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String): Result<List<Int>> = repository.searchPage(query)
}
