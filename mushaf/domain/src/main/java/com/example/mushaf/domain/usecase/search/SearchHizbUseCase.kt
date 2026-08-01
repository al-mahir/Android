package com.example.mushaf.domain.usecase.search

import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result

class SearchHizbUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(query: String): Result<List<Hizb>> = repository.searchHizb(query)
}
