package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.repository.RecitationRepository

class CancelDownloadRecitationUseCase(
    private val repository: RecitationRepository
) {
    operator fun invoke(reciterId: Int, surahNumber: Int?) {
        repository.cancelDownloadRecitation(reciterId, surahNumber)
    }
}
