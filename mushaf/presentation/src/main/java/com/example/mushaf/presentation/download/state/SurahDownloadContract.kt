package com.example.mushaf.presentation.download.state

import com.example.mushaf.domain.model.DownloadStatus

data class SurahDownloadUiState(
    val reciterId: Int = -1,
    val reciterName: com.example.mushaf.domain.model.LocalizedText? = null,
    val surahs: List<SurahDownloadItem> = emptyList(),
)

data class SurahDownloadItem(
    val surahNumber: Int,
    val name: String,
    val status: DownloadStatus?,
    val isDownloading: Boolean = false,
    val progress: Int = 0
)

sealed interface SurahDownloadIntent {
    data class DownloadSurah(val surahNumber: Int) : SurahDownloadIntent
    data class CancelDownload(val surahNumber: Int) : SurahDownloadIntent
}

sealed interface SurahDownloadEffect {
    data class ShowError(val messageRes: Int) : SurahDownloadEffect
}
