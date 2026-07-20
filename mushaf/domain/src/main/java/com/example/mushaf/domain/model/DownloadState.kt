package com.example.mushaf.domain.model


sealed interface DownloadState {

    data object NotDownloaded : DownloadState

    data class Downloading(val progress: Float) : DownloadState

    data object Downloaded : DownloadState

    data class Failed(val reason: DownloadFailure) : DownloadState
}

enum class DownloadFailure {
    NO_CONNECTION,
    INSUFFICIENT_STORAGE,
    UNKNOWN,
}
