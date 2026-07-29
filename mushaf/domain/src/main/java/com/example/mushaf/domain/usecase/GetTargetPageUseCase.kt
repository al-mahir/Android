package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result

class GetTargetPageUseCase(private val repository: MushafRepository) {
    suspend fun forSurah(surahNumber: Int): Result<Int?> = repository.getSurahStartingPage(surahNumber)
    suspend fun forAyah(surahNumber: Int, ayahNumber: Int): Result<Int?> = repository.getAyahPage(surahNumber, ayahNumber)
    suspend fun forJuz(juzNumber: Int): Result<Int?> = repository.getJuzStartingPage(juzNumber)
}
