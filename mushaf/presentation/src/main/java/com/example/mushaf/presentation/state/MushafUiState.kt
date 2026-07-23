package com.example.mushaf.presentation.state

import com.example.mushaf.domain.model.MushafConstants
import com.iti.domain.model.recitation.RecitationSessionSummary
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.MushafWord
import com.example.mushaf.domain.model.ReadingMode
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.presentation.audio.AudioState

data class MushafUiState(
    val currentPage: Int = MushafConstants.FIRST_PAGE,
    val pages: Map<Int, MushafPage> = emptyMap(),
    val failedPages: Set<Int> = emptySet(),
    val isTajweedEnabled: Boolean = true,
    val highlightedWordId: String? = null,
    val pageCount: Int = MushafConstants.LAST_PAGE,
    val isFollowAlongActive: Boolean = false,
    val mushafMode: MushafMode = MushafMode.READING,
    val areBarsVisible: Boolean = true,
    val areAyahsVisible: Boolean = true,
    val isRecordingActive: Boolean = false,
    val revealedWordIds: Set<String> = emptySet(),


    val micLevel: Float = 0f,
     
    val isSpeechDetected: Boolean = false,
    val captureError: CaptureError? = null,

    val isTajweedGradingEnabled: Boolean = true,

    val canGradeTajweed: Boolean = true,

    val liveCorrection: LiveCorrectionUiState = LiveCorrectionUiState(),
     
    val sessionSummary: RecitationSessionSummary? = null,

    
    val currentReciter: Reciter? = null,
    val audioState: AudioState = AudioState.IDLE,
    val playingPage: Int? = null,
    val playbackSpeed: Float = 1.0f,
    val availableReciters: List<Reciter> = emptyList(),

    // Surah Picker
    val showSurahPicker: Boolean = false,

    // Tajweed Legend
    val showTajweedLegend: Boolean = false,

    // Tafsir
    val tafsirState: TafsirState = TafsirState.Idle
) {
    val readingMode: ReadingMode get() = ReadingMode.from(isTajweedEnabled)
    val page: MushafPage? get() = pages[currentPage]
    val isLoading: Boolean get() = currentPage !in pages && currentPage !in failedPages
    val currentSurahNumber: Int get() = MushafConstants.surahForPage(currentPage)

    fun pageState(pageNumber: Int): PageLoadState = when {
        pages.containsKey(pageNumber) -> PageLoadState.Loaded(pages.getValue(pageNumber))
        pageNumber in failedPages -> PageLoadState.Failed
        else -> PageLoadState.Loading
    }

    fun wordsForCurrentPage(): List<MushafWord> =
        page?.lines?.flatMap { it.words } ?: emptyList()
}

sealed interface PageLoadState {
    data class Loaded(val page: MushafPage) : PageLoadState
    data object Loading : PageLoadState
    data object Failed : PageLoadState
}

sealed interface TafsirState {
    data object Idle : TafsirState
    data class Loading(val surah: Int, val ayah: Int) : TafsirState
    data class Success(val tafsir: com.example.mushaf.domain.model.TafsirResult) : TafsirState
    data class Error(val message: String) : TafsirState
}

enum class CaptureError {
    PERMISSION_DENIED,
    MICROPHONE_UNAVAILABLE,
    SERVICE_UNREACHABLE,
}
