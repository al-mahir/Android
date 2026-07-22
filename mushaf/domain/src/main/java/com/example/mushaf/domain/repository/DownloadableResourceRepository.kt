package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.DownloadableResource
import com.example.mushaf.domain.model.ResourceKind
import kotlinx.coroutines.flow.Flow


interface DownloadableResourceRepository {

    fun observeResources(kind: ResourceKind): Flow<List<DownloadableResource>>

    suspend fun startDownload(id: String)

    suspend fun cancelDownload(id: String)

    suspend fun deleteDownload(id: String)
}
