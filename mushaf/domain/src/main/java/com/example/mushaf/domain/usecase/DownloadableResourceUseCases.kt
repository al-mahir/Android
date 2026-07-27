package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.DownloadableResource
import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.domain.repository.DownloadableResourceRepository
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow

class ObserveDownloadableResourcesUseCase(
    private val repository: DownloadableResourceRepository,
) {
    operator fun invoke(kind: ResourceKind): Flow<Result<List<DownloadableResource>>> =
        repository.observeResources(kind)
}

class StartResourceDownloadUseCase(
    private val repository: DownloadableResourceRepository,
) {
    suspend operator fun invoke(id: String) = repository.startDownload(id)
}

class CancelResourceDownloadUseCase(
    private val repository: DownloadableResourceRepository,
) {
    suspend operator fun invoke(id: String) = repository.cancelDownload(id)
}

class DeleteResourceDownloadUseCase(
    private val repository: DownloadableResourceRepository,
) {
    suspend operator fun invoke(id: String) = repository.deleteDownload(id)
}
