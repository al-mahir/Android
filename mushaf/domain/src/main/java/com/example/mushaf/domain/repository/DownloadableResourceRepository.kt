package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.DownloadableResource
import com.example.mushaf.domain.model.ResourceKind
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow


interface DownloadableResourceRepository {

    fun observeResources(kind: ResourceKind): Flow<Result<List<DownloadableResource>>>

    suspend fun startDownload(id: String): Result<Unit>

    suspend fun cancelDownload(id: String): Result<Unit>

    suspend fun deleteDownload(id: String): Result<Unit>
}
