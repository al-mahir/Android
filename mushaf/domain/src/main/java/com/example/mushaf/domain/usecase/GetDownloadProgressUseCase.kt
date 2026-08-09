package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.DownloadStatus
import com.example.mushaf.domain.repository.RecitationRepository
import kotlinx.coroutines.flow.Flow

class GetDownloadProgressUseCase(
    private val repository: RecitationRepository
) {
    operator fun invoke(reciterId: Int): Flow<List<DownloadStatus>> {
        return repository.observeDownloadProgress(reciterId)
    }
}
