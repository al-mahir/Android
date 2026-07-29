package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.TafsirBook
import com.example.mushaf.domain.repository.MushafRepository

class GetAvailableTafsirBooksUseCase(
    private val repository: MushafRepository,
) {
    suspend operator fun invoke(): List<TafsirBook> =
        repository.getAvailableTafsirBooks()
}
