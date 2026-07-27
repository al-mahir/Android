package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.TafsirResult
import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result

class GetTafsirForAyahUseCase(
    private val repository: MushafRepository
) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int): Result<TafsirResult?> {
        return repository.getTafsirForAyah(surahNumber, ayahNumber)
    }
}
