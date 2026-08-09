package com.example.mushaf.presentation.state

import com.example.designsystem.text.UiText
import com.example.mushaf.domain.model.MushafConstants
import com.iti.domain.model.recitation.RecitationSessionSummary
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.MushafWord
import com.example.mushaf.domain.model.ReadingMode
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.presentation.audio.AudioState

import com.example.mushaf.presentation.muallem.MuallemPhase
import com.example.mushaf.presentation.muallem.MuallemSessionState

data class MushafUiState(
    val currentPage: Int = MushafConstants.FIRST_PAGE,
    val pages: Map<Int, MushafPage> = emptyMap(),
    val failedPages: Set<Int> = emptySet(),
    val isTajweedEnabled: Boolean = true,
    /**
     * The word the page actually paints. During a live session it is whichever of
     * [confirmedWordId] and [predictedWordId] is further along; outside one it is driven directly
     * by playback, the memorisation veil or a tap.
     */
    val highlightedWordId: String? = null,
    /**
     * Where the server says the reciter is. Ground truth: grading, mistake marks and page turns
     * all follow this and never [predictedWordId].
     */
    val confirmedWordId: String? = null,
    /**
     * Where the on-device model thinks the reciter is, always at or ahead of [confirmedWordId].
     *
     * Kept separate from the confirmed cursor rather than folded into one field, because a single
     * field means every arriving chunk overwrites a prediction that has legitimately run ahead of
     * it — the highlight snaps backwards a word or two on every server reply, which reads as
     * stutter and is worse than not predicting at all.
     */
    val predictedWordId: String? = null,
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

    /**
     * The practice mode, and with it the engine: tajwīd grading needs the correcting engine, and
     * memorisation-only runs the follow-along one. One value, reachable from the on-page toggle
     * and from Recite Settings alike — see [com.example.mushaf.domain.model.recite.RecitationSettings.withTajweedGrading].
     */
    val isTajweedGradingEnabled: Boolean = true,

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
    val tafsirState: TafsirState = TafsirState.Idle,
    val selectedTafsirKey: String = "mukhtasar",
    val availableTafsirBooks: List<com.example.mushaf.domain.model.TafsirBook> = emptyList(),

    // Ayah action sheet
    val ayahActionSheet: AyahActionSheetState? = null,
    val ayahNoteEditorOpen: Boolean = false,

    // User Guide
    val showUserGuide: Boolean = false,
    val guideStep: Int = 1,

    val isOffline: Boolean = false,

    // Bookmarks
    val bookmarkedPages: Set<Int> = emptySet(),
    val bookmarkedAyahs: Set<Pair<Int, Int>> = emptySet(),

    // Mu'allem session
    val showMuallemSetup: Boolean = false,
    val muallemSession: MuallemSessionState? = null,
) {
    val readingMode: ReadingMode get() = ReadingMode.from(isTajweedEnabled)

    /**
     * Whether the mic button is the user's to press.
     *
     * In Mu'allem the session drives the mic: it opens by itself when the sheikh's recitation ends
     * and closes when the repeat is done. A tap outside that window — while the sheikh is reciting,
     * during the feedback pause, or before a session exists — would open a second, unmanaged
     * capture and desync the whole flow, so the button is inert there. (Re-tapping the Mu'allem
     * tab is the way back to the setup sheet when no session is running.)
     */
    val isMicEnabled: Boolean
        get() = mushafMode != MushafMode.MUALLEM ||
                muallemSession?.phase is MuallemPhase.UserRecording

    val isCurrentPageBookmarked: Boolean get() = currentPage in bookmarkedPages
    val page: MushafPage? get() = pages[currentPage]
    val isLoading: Boolean get() = currentPage !in pages && currentPage !in failedPages
    val currentSurahNumber: Int get() = MushafConstants.surahForPage(currentPage)
    val currentJuzNumber: Int get() = MushafConstants.juzForPage(currentPage)
    /** True when currentPage is odd → right-hand face in a printed Mushaf. */
    val isRightPage: Boolean get() = currentPage % 2 == 1
    /** 1-based hizb-quarter index (1..240) for the current page. */
    val currentHizbQuarter: Int get() = MushafConstants.hizbQuarterForPage(currentPage)
    /** Quarter position within the current hizb: 1, 2, 3, or 4. */
    val hizbQuarterInHizb: Int get() = ((currentHizbQuarter - 1) % 4) + 1

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
    data class Error(val message: UiText) : TafsirState
}

data class AyahActionSheetState(
    val surahNumber: Int,
    val ayahNumber: Int,
    val ayahText: String = "",
    val note: com.example.mushaf.domain.model.AyahNote? = null,
)

enum class CaptureError {
    PERMISSION_DENIED,
    MICROPHONE_UNAVAILABLE,
    SERVICE_UNREACHABLE,
}
