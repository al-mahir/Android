package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.repository.MushafRepository

class GetTargetPageUseCase(private val repository: MushafRepository) {
    suspend fun forSurah(surahNumber: Int): Int? = repository.getSurahStartingPage(surahNumber)
    suspend fun forAyah(surahNumber: Int, ayahNumber: Int): Int? = repository.getAyahPage(surahNumber, ayahNumber)
    suspend fun forJuz(juzNumber: Int): Int? = repository.getJuzStartingPage(juzNumber)
}
