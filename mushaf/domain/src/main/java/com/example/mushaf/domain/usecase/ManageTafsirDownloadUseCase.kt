package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.repository.MushafRepository

class ManageTafsirDownloadUseCase(
    private val repository: MushafRepository,
) {
    suspend fun download(tafsirKey: String, downloadUrl: String) {
        repository.downloadTafsirBook(tafsirKey, downloadUrl)
    }

    fun delete(tafsirKey: String) {
        repository.deleteTafsirBook(tafsirKey)
    }
}
