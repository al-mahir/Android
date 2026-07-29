package com.example.mushaf.domain.usecase

import android.util.Log
import com.example.mushaf.domain.model.TafsirResult
import com.example.mushaf.domain.repository.MushafRepository

class GetTafsirForAyahUseCase(
    private val repository: MushafRepository,
) {
    /**
     * Try the remote API first; silently fall back to the offline SQLite DB
     * if the network call throws any exception.
     */
    suspend operator fun invoke(
        surahNumber: Int,
        ayahNumber: Int,
        tafsirKey: String = "ibn-kathir",
        lang: String = "ar",
    ): TafsirResult? {
        return try {
            repository.getTafsirFromApi(surahNumber, ayahNumber, lang, tafsirKey)
                ?: repository.getTafsirForAyah(surahNumber, ayahNumber)
        } catch (e: Exception) {
            Log.w("GetTafsirForAyahUseCase", "Online fetch failed, falling back to local DB", e)
            repository.getTafsirForAyah(surahNumber, ayahNumber)
        }
    }
}
