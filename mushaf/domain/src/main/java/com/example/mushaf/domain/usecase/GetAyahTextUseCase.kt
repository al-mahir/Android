package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result

class GetAyahTextUseCase(private val repository: MushafRepository) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int): Result<String?> =
        repository.getAyahText(surahNumber, ayahNumber)
}
