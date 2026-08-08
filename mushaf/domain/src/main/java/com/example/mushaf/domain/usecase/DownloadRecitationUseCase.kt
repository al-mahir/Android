package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.repository.RecitationRepository

class DownloadRecitationUseCase(
    private val repository: RecitationRepository
) {
    suspend operator fun invoke(reciterId: Int, surahNumber: Int?) {
        repository.downloadRecitation(reciterId, surahNumber)
    }
}
