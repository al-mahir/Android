package com.example.mushaf.domain.model

data class DownloadStatus(
    val id: String,
    val reciterId: Int,
    val surahId: Int?,
    val progress: Int,
    val state: String,
    val errorMessage: String?
) {
    val isDownloading: Boolean get() = state == STATE_DOWNLOADING
    val isCompleted: Boolean get() = state == STATE_COMPLETED || state == STATE_DOWNLOADED

    companion object {
        const val STATE_PENDING = "PENDING"
        const val STATE_DOWNLOADING = "DOWNLOADING"
        const val STATE_COMPLETED = "COMPLETED"
        const val STATE_DOWNLOADED = "DOWNLOADED"
        const val STATE_ERROR = "ERROR"
    }
}
