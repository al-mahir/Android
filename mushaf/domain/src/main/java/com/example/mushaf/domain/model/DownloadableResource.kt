package com.example.mushaf.domain.model


data class LocalizedText(
    val arabic: String,
    val english: String,
)

enum class ResourceKind {
    RECITER,
    TAFSEER,
    TRANSLATION,
}


data class DownloadableResource(
    val id: String,
    val kind: ResourceKind,
    val name: LocalizedText,
    val subtitle: LocalizedText? = null,
    val sizeBytes: Long = 0L,
    val state: DownloadState = DownloadState.NotDownloaded,
)
