package com.example.mushaf.presentation.download.state

import com.example.mushaf.domain.model.DownloadableResource
import com.example.mushaf.domain.model.ResourceKind


data class DownloadsUiState(
    val kind: ResourceKind = ResourceKind.RECITER,
    val isLoading: Boolean = true,
    val resources: List<DownloadableResource> = emptyList(),
    val errorMessageRes: Int? = null,
    val pendingDeletion: DownloadableResource? = null,
    val pendingFullDownload: DownloadableResource? = null,
) {
    val isEmpty: Boolean get() = !isLoading && resources.isEmpty() && errorMessageRes == null
}

sealed interface DownloadsIntent {
    data class DownloadClicked(val id: String) : DownloadsIntent
    data object DownloadFullConfirmed : DownloadsIntent
    data object DownloadFullDismissed : DownloadsIntent
    data class CancelClicked(val id: String) : DownloadsIntent
    data class DeleteClicked(val id: String) : DownloadsIntent
    data object DeleteConfirmed : DownloadsIntent
    data object DeleteDismissed : DownloadsIntent
    data object Retry : DownloadsIntent
    data class ItemClicked(val id: String) : DownloadsIntent
}

sealed interface DownloadsEffect {
    data class ShowMessage(val messageRes: Int) : DownloadsEffect
    data class NavigateToSurahList(val reciterId: String) : DownloadsEffect
}
