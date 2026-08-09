package com.example.mushaf.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.GetAyahTextUseCase
import com.example.mushaf.domain.usecase.ObserveAyahNoteUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.domain.usecase.GetRecitersUseCase
import com.example.mushaf.domain.usecase.GetAyahTimingsUseCase
import com.example.mushaf.domain.usecase.GetTargetPageUseCase
import com.example.mushaf.domain.usecase.StartLiveRecitationUseCase
import com.example.mushaf.domain.usecase.ObserveRecitationSettingsUseCase
import com.example.mushaf.domain.usecase.UpdateRecitationSettingsUseCase
import com.example.mushaf.domain.model.recite.RecitationSettings
import com.iti.domain.usecase.SaveRecitationSessionUseCase
import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationChunk
import com.example.mushaf.domain.model.recite.RecitationControl
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMatch
import com.example.mushaf.domain.model.recite.RecitationSessionRecorder
import com.example.mushaf.domain.model.recite.mergedWith
import com.example.mushaf.domain.model.recite.RecitationWordOrder
import com.example.mushaf.domain.model.recite.local.LocalTranscript
import com.example.mushaf.domain.model.recite.local.PhonemeCursorTracker
import com.example.mushaf.domain.repository.LocalWordCorpusRepository
import com.example.mushaf.domain.repository.ReferencePhonemeRepository
import com.example.mushaf.presentation.state.ChunkOutcome
import com.example.mushaf.presentation.state.LiveCorrectionUiState
import com.example.mushaf.presentation.state.CaptureError
import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.domain.model.SurahCatalog
import com.example.mushaf.presentation.audio.AudioPlayer
import com.example.mushaf.presentation.audio.AudioState
import com.example.mushaf.presentation.highlight.AudioHighlightDriver
import com.example.mushaf.presentation.highlight.HighlightDriver
import com.example.mushaf.presentation.highlight.SimulatedHighlightDriver
import com.example.mushaf.presentation.muallem.MuallemPhase
import com.example.mushaf.presentation.muallem.MuallemRepeatFeedback
import com.example.mushaf.presentation.muallem.MuallemSessionState
import com.example.mushaf.presentation.state.MushafEffect
import com.iti.domain.core.Result
import com.iti.domain.core.getOrNull
import com.example.mushaf.domain.usecase.DownloadRecitationUseCase
import com.example.mushaf.domain.usecase.GetTafsirForAyahUseCase
import com.example.mushaf.domain.usecase.ManageTafsirDownloadUseCase
import com.example.mushaf.domain.usecase.ObserveAvailableTafsirBooksUseCase
import com.example.mushaf.domain.usecase.SetFirstMushafLaunchCompletedUseCase
import com.example.mushaf.presentation.core.error.toUiText
import com.example.mushaf.presentation.state.MushafIntent
import com.example.mushaf.presentation.state.MushafUiState
import com.example.mushaf.presentation.R
import com.iti.domain.connectivity.ConnectivityObserver
import com.iti.domain.usecase.bookmark.ObserveBookmarksUseCase
import com.iti.domain.usecase.bookmark.ToggleBookmarkUseCase
import com.iti.domain.usecase.settings.ObserveAppPreferencesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class MushafViewModel(
    private val getPage: GetPageUseCase,
    private val observeReaderPreferences: ObserveReaderPreferencesUseCase,
    private val setTajweedEnabled: SetTajweedEnabledUseCase,
    private val setFirstMushafLaunchCompleted: SetFirstMushafLaunchCompletedUseCase,
    private val saveLastPage: SaveLastPageUseCase,
    private val getReciters: GetRecitersUseCase,
    private val getAyahTimings: GetAyahTimingsUseCase,
    private val getTafsirForAyah: GetTafsirForAyahUseCase,
    private val getTargetPage: GetTargetPageUseCase,
    private val playbackManager: AudioPlayer,
    private val startLiveRecitation: StartLiveRecitationUseCase,
    private val saveRecitationSession: SaveRecitationSessionUseCase,
    private val observeRecitationSettings: ObserveRecitationSettingsUseCase,
    private val updateRecitationSettings: UpdateRecitationSettingsUseCase,
    private val downloadRecitation: DownloadRecitationUseCase,
    private val localWordCorpusRepository: LocalWordCorpusRepository,
    private val referencePhonemeRepository: ReferencePhonemeRepository,
    private val observeAvailableTafsirBooks: ObserveAvailableTafsirBooksUseCase,
    private val manageTafsirDownload: ManageTafsirDownloadUseCase,
    private val observeAppPreferences: ObserveAppPreferencesUseCase,
    private val observeAyahNote: ObserveAyahNoteUseCase,
    private val upsertAyahNote: com.example.mushaf.domain.usecase.UpsertAyahNoteUseCase,
    private val deleteAyahNote: com.example.mushaf.domain.usecase.DeleteAyahNoteUseCase,
    private val getAyahText: GetAyahTextUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val observeBookmarks: ObserveBookmarksUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MushafUiState())
    val state: StateFlow<MushafUiState> = _state.asStateFlow()

    private val _effects = Channel<MushafEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val simulatedHighlightDriver = SimulatedHighlightDriver(viewModelScope)
    private val audioHighlightDriver = AudioHighlightDriver(viewModelScope, playbackManager)

    private val highlightDriver: HighlightDriver
        get() = if (_state.value.mushafMode == MushafMode.LISTEN) audioHighlightDriver else simulatedHighlightDriver

    private var initialized = false
    private val loadJobs = mutableMapOf<Int, Job>()
    private var sessionJob: Job? = null
    private var noteJob: Job? = null
    private var pendingStartDetection = false
    private var detectionJob: Job? = null
    private var controlChannel: Channel<RecitationControl>? = null

    // ── Mu'allem ──────────────────────────────────────────────────────────────
    private var muallemSessionJob: Job? = null
    private var muallemSpeechStartTime: Long = 0L
    private var muallemControlChannel: Channel<RecitationControl>? = null
    private var muallemAutoAdvanceJob: Job? = null
    private var muallemStartedAtEpochMs: Long = 0L
    private var muallemAyahWordCount: Int? = null
    private var muallemRepeatFinishing: Boolean = false

    private var lastCursor: RecitationCursor? = null
    private var reconnectAttempts = 0


    private var isFinishing = false
    private var smoothedMicLevel = 0f


    private var seekOnPageLoad: Int? = null

    private val localCursorTracker = PhonemeCursorTracker()

    private var sessionStartedAtEpochMs = 0L


    private var pendingRestart: SessionRestart? = null

    private var recitationSettings = RecitationSettings()

    private enum class SessionRestart {
        BOUNDARY,

        SETTINGS,
    }

    private companion object {
        const val TAG = "Mushaf"


        const val MIC_LEVEL_SMOOTHING = 0.3f

        const val MIC_LEVEL_GAIN = 4f

        const val CAPTURE_LOG_INTERVAL_MS = 1_000L

        const val CLIPPING_THRESHOLD = 0.99f

        const val MAX_RECONNECT_ATTEMPTS = 3
        const val RECONNECT_DELAY_MS = 1_000L

        // Kept small on purpose: a wide window (e.g. a whole page, 100+ words) means almost any
        // recognized token - even a garbled one - accidentally matches *something* in it, since
        // short/common Arabic words repeat constantly. Confirmed on a real device in an earlier
        // round of this feature: with a 127-word window, 57 of 61 recognized tokens matched
        // something even under strict matching. A small window anchored at the last known-good
        // position keeps the candidate set tight enough that a match is actually meaningful.
        const val LOCAL_TRACKER_WINDOW_WORDS = 12
    }

    init {
        simulatedHighlightDriver.currentWordId
            .onEach { updateWordHighlight(it) }
            .launchIn(viewModelScope)

        audioHighlightDriver.currentWordId
            .onEach { updateWordHighlight(it) }
            .launchIn(viewModelScope)

        playbackManager.audioState
            .onEach { state ->
                _state.update { it.copy(audioState = state) }
                if (state == AudioState.ENDED && _state.value.mushafMode == MushafMode.LISTEN && _state.value.isFollowAlongActive) {
                    val currentlyPlaying = _state.value.playingPage ?: _state.value.currentPage
                    val nextPage = currentlyPlaying + 1

                    if (nextPage <= MushafConstants.LAST_PAGE) {
                        viewModelScope.launch {
                            if (!_state.value.pages.containsKey(nextPage)) {
                                getPage(nextPage).first().getOrNull()?.let { loaded ->
                                    _state.update { it.copy(pages = it.pages + (nextPage to loaded)) }
                                }
                            }

                            val nextPageData = _state.value.pages[nextPage]
                            if (nextPageData != null) {
                                val isFirstPageOfSurah = nextPageData.lines.any { it.type == LineType.SURAH_NAME }
                                if (isFirstPageOfSurah) {
                                    stopFollowAlong()
                                } else {
                                    startFollowAlong(nextPage)
                                    if (_state.value.currentPage == currentlyPlaying) {
                                        onIntent(MushafIntent.LoadPage(nextPage))
                                    }
                                }
                            }
                        }
                    } else {
                        stopFollowAlong()
                    }
                }
                // Mu'allem: sheikh audio ended → open mic for user's repeat
                if (state == AudioState.ENDED
                    && _state.value.mushafMode == MushafMode.MUALLEM
                    && _state.value.muallemSession?.phase == MuallemPhase.SheikhPlaying
                ) {
                    startMuallemRepeat()
                }
            }
            .launchIn(viewModelScope)

        playbackManager.playbackSpeed
            .onEach { speed -> _state.update { it.copy(playbackSpeed = speed) } }
            .launchIn(viewModelScope)

        playbackManager.externalCommands
            .onEach { command ->
                when (command) {
                    "ACTION_NEXT_SURAH" -> onIntent(MushafIntent.NextSurahAudio)
                    "ACTION_PREV_SURAH" -> onIntent(MushafIntent.PrevSurahAudio)
                }
            }
            .launchIn(viewModelScope)

        loadReciters()

        observeRecitationSettings()
            .catch { throwable ->
                Log.e(TAG, "Failed to read recitation settings; using server defaults", throwable)
            }
            .onEach { settings ->
                recitationSettings = settings
                _state.update {
                    it.copy(isTajweedGradingEnabled = settings.gradesTajweed)
                }
            }
            .launchIn(viewModelScope)

        observeReaderPreferences()
            .catch { throwable ->
                Log.e(TAG, "Failed to read reader preferences; using defaults", throwable)
                if (!initialized) {
                    initialized = true
                    onIntent(MushafIntent.LoadPage(MushafConstants.FIRST_PAGE))
                }
            }
            .onEach { prefs ->
                Log.d(TAG, "Preferences: tajweed=${prefs.tajweedEnabled}, lastPage=${prefs.lastPage}")
                _state.update {
                    it.copy(
                        isTajweedEnabled = prefs.tajweedEnabled,
                        showUserGuide = prefs.isFirstMushafLaunch
                    )
                }
                if (!initialized) {
                    initialized = true
                    onIntent(MushafIntent.LoadPage(prefs.lastPage))
                }
            }
            .launchIn(viewModelScope)

        // Eagerly load available Tafsir books from the backend
        fetchAvailableTafsirBooks()

        connectivityObserver.status
            .onEach { status ->
                _state.update { it.copy(isOffline = status == com.iti.domain.connectivity.ConnectivityStatus.Unavailable) }
            }
            .launchIn(viewModelScope)

        observeBookmarks(com.iti.domain.model.BookmarkType.PAGE)
            .onEach { result ->
                if (result is Result.Success) {
                    val pages = result.data.mapNotNull { it.pageNumber }.toSet()
                    _state.update { it.copy(bookmarkedPages = pages) }
                }
            }
            .launchIn(viewModelScope)

        observeBookmarks(com.iti.domain.model.BookmarkType.AYAH)
            .onEach { result ->
                if (result is Result.Success) {
                    val ayahs = result.data.mapNotNull { bookmark ->
                        val surah = bookmark.surahNumber
                        val ayah = bookmark.ayahNumber
                        if (surah != null && ayah != null) surah to ayah else null
                    }.toSet()
                    _state.update { it.copy(bookmarkedAyahs = ayahs) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: MushafIntent) {
        when (intent) {
            is MushafIntent.LoadPage -> loadPage(intent.page)
            is MushafIntent.OpenAtPage -> {


                Log.d(TAG, "OpenAtPage(${intent.page}) — suppressing last-page restore")
                initialized = true
                loadPage(intent.page)
            }
            is MushafIntent.ToggleTajweed -> toggleTajweed(intent.enabled)
            is MushafIntent.HighlightWord -> _state.update { it.copy(highlightedWordId = intent.wordId) }
            MushafIntent.StartFollowAlongPreview -> startFollowAlong()
            MushafIntent.StopFollowAlongPreview -> stopFollowAlong()
            MushafIntent.Retry -> requestPage(_state.value.currentPage)
            MushafIntent.ToggleBars -> _state.update { it.copy(areBarsVisible = !it.areBarsVisible) }
            is MushafIntent.SetMode -> setMode(intent.mode)
            MushafIntent.ToggleAyahVisibility -> toggleAyahVisibility()
            MushafIntent.RevealNextWord -> revealNextWord()
            MushafIntent.RevealNextAyah -> revealNextAyah()
            MushafIntent.ToggleRecording -> toggleRecording()
            is MushafIntent.SetTajweedGrading -> setTajweedGrading(intent.enabled)
            is MushafIntent.CaptureFailed -> failCapture(intent.error)
            MushafIntent.DismissCaptureError -> _state.update { it.copy(captureError = null) }
            is MushafIntent.SelectMistake -> _state.update {
                it.copy(liveCorrection = it.liveCorrection.copy(selectedMistakeWordId = intent.wordId))
            }
            MushafIntent.DismissSessionSummary -> _state.update { it.copy(sessionSummary = null) }
            MushafIntent.FinishAndStartNewSession -> finishAndStartNewSession()
            is MushafIntent.SelectCandidate -> selectCandidate(intent.position)
            MushafIntent.DismissCandidates -> clearCandidates()
            MushafIntent.DismissEngineNotice -> _state.update {
                it.copy(liveCorrection = it.liveCorrection.copy(engineSubstituted = false))
            }

            is MushafIntent.SelectReciter -> selectReciter(intent.reciter)
            MushafIntent.PlayPauseAudio -> playPauseAudio()
            is MushafIntent.SetAudioSpeed -> playbackManager.setSpeed(intent.speed)
            is MushafIntent.SeekAudio -> playbackManager.seekTo(intent.positionMs)
            MushafIntent.NextSurahAudio -> {
                val next = (_state.value.currentSurahNumber + 1).coerceAtMost(114)
                if (next != _state.value.currentSurahNumber) {
                    navigateToSurah(next)
                }
            }
            MushafIntent.PrevSurahAudio -> {
                val prev = (_state.value.currentSurahNumber - 1).coerceAtLeast(1)
                if (prev != _state.value.currentSurahNumber) {
                    navigateToSurah(prev)
                }
            }

            // Offline Downloads
            is MushafIntent.DownloadRecitation -> {
                viewModelScope.launch {
                    downloadRecitation(intent.reciterId, intent.surahNumber)
                }
            }

            // Surah Picker
            MushafIntent.ShowSurahPicker -> _state.update { it.copy(showSurahPicker = true) }
            MushafIntent.HideSurahPicker -> _state.update { it.copy(showSurahPicker = false) }
            is MushafIntent.NavigateToSurah -> navigateToSurah(intent.surahNumber)

            // Tajweed Legend
            MushafIntent.ShowTajweedLegend -> _state.update { it.copy(showTajweedLegend = true) }
            MushafIntent.HideTajweedLegend -> _state.update { it.copy(showTajweedLegend = false) }

            // Tafsir
            is MushafIntent.LoadTafsir -> loadTafsir(intent.surah, intent.ayah)
            MushafIntent.DismissTafsir -> _state.update { it.copy(tafsirState = com.example.mushaf.presentation.state.TafsirState.Idle) }
            is MushafIntent.ChangeTafsirSource -> {
                _state.update { it.copy(selectedTafsirKey = intent.tafsirKey) }
                val currentTafsirState = _state.value.tafsirState
                if (currentTafsirState is com.example.mushaf.presentation.state.TafsirState.Success) {
                    loadTafsir(currentTafsirState.tafsir.surahNumber, currentTafsirState.tafsir.ayahNumber)
                } else if (currentTafsirState is com.example.mushaf.presentation.state.TafsirState.Loading) {
                    loadTafsir(currentTafsirState.surah, currentTafsirState.ayah)
                }
            }
            MushafIntent.RefreshTafsirBooks -> Unit // No-op, it's observed
            is MushafIntent.DownloadTafsir -> {
                viewModelScope.launch {
                    manageTafsirDownload.download(intent.tafsirKey, intent.downloadUrl)
                }
            }
            is MushafIntent.DeleteTafsir -> manageTafsirDownload.delete(intent.tafsirKey)

            // Ayah action sheet
            is MushafIntent.ShowAyahActions -> showAyahActions(intent.surah, intent.ayah)
            MushafIntent.DismissAyahActions -> dismissAyahActions()
            MushafIntent.OpenAyahNoteEditor -> openAyahNoteEditor()
            MushafIntent.CloseAyahNoteEditor -> _state.update { it.copy(ayahNoteEditorOpen = false) }
            is MushafIntent.SaveAyahNote -> saveAyahNote(intent.text)
            MushafIntent.DeleteAyahNote -> removeAyahNote()
            MushafIntent.CopyAyah -> copyAyah()
            
            // User Guide
            MushafIntent.GuideNextStep -> {
                val currentStep = _state.value.guideStep
                if (currentStep < 6) {
                    _state.update { it.copy(guideStep = currentStep + 1) }
                } else {
                    viewModelScope.launch { setFirstMushafLaunchCompleted() }
                }
            }
            MushafIntent.DismissGuide -> {
                viewModelScope.launch { setFirstMushafLaunchCompleted() }
            }
            MushafIntent.TogglePageBookmark -> {
                viewModelScope.launch {
                    val page = _state.value.currentPage
                    val bookmark = com.iti.domain.model.Bookmark(
                        id = "",
                        type = com.iti.domain.model.BookmarkType.PAGE,
                        pageNumber = page,
                        createdAtEpochMillis = System.currentTimeMillis()
                    )
                    toggleBookmarkUseCase(bookmark)
                }
            }
            is MushafIntent.ToggleAyahBookmark -> {
                viewModelScope.launch {
                    val bookmark = com.iti.domain.model.Bookmark(
                        id = "",
                        type = com.iti.domain.model.BookmarkType.AYAH,
                        surahNumber = intent.surah,
                        ayahNumber = intent.ayah,
                        pageNumber = _state.value.currentPage,
                        createdAtEpochMillis = System.currentTimeMillis()
                    )
                    toggleBookmarkUseCase(bookmark)
                }
            }

            // Mu'allem
            MushafIntent.ShowMuallemSetup -> _state.update { it.copy(showMuallemSetup = true) }
            MushafIntent.DismissMuallemSetup -> _state.update { it.copy(showMuallemSetup = false) }
            is MushafIntent.StartMuallemSession -> startMuallemSession(
                intent.surah, intent.startAyah, intent.endAyah, intent.repeatCount, intent.difficulty
            )
            MushafIntent.StopMuallemSession -> stopMuallemSession(showSummaryIfAny = true)
            MushafIntent.MuallemRepeatDone -> finishMuallemRepeat()
        }
    }

    private fun updateWordHighlight(wordId: String?) {
        _state.update { it.copy(highlightedWordId = wordId).revealing(wordId) }
    }

    /**
     * Uncovers [wordId] while the memorisation veil is on, so a word the user has just recited (or
     * that playback has just reached) is actually readable instead of staying blank under its own
     * highlight.
     *
     * Everything from the top of the page up to [wordId] is uncovered, not just [wordId] itself:
     * the cursor routinely lands mid-page without having reported every word before it — most
     * obviously right after a page turn, where [loadPage] has just re-hidden the page and the first
     * confirmed word can be several words in. Revealing the whole prefix keeps the page reading as
     * "everything I have recited so far", never a scatter of visible words over blank gaps.
     *
     * Already-revealed ids are kept, so a cursor that moves backwards (a correction, a seek) never
     * re-hides text, and manual [revealNextWord] / [revealNextAyah] steps ahead of the cursor stand.
     *
     * No-op when ayahs are visible, so it is safe to call from every cursor update.
     */
    private fun MushafUiState.revealing(wordId: String?): MushafUiState {
        if (areAyahsVisible || wordId == null) return this
        val words = wordsForCurrentPage()
        val target = words.indexOfFirst { it.id == wordId }
        if (target < 0) return copy(revealedWordIds = revealedWordIds + wordId)

        return copy(
            revealedWordIds = revealedWordIds + words.take(target + 1).map { it.id },
        )
    }

    private fun navigateToSurah(surahNumber: Int) {
        val wasPlaying = _state.value.audioState == com.example.mushaf.presentation.audio.AudioState.PLAYING
        if (wasPlaying) {
            playbackManager.pause()
        }

        val idx = surahNumber - 1
        val startPage = com.example.mushaf.domain.model.MushafConstants.SURAH_START_PAGES
            .getOrElse(idx) { com.example.mushaf.domain.model.MushafConstants.FIRST_PAGE }
        _state.update { it.copy(showSurahPicker = false) }
        loadPage(startPage)

        if (wasPlaying) {
            viewModelScope.launch {
                var attempts = 0
                while (_state.value.pages[startPage] == null && attempts < 50) {
                    kotlinx.coroutines.delay(100)
                    attempts++
                }
                if (_state.value.pages[startPage] != null) {
                    startFollowAlong(startPage, targetSurahNumber = surahNumber)
                }
            }
        }
    }

    private fun loadTafsir(surah: Int, ayah: Int) {
        val tafsirKey = _state.value.selectedTafsirKey
        _state.update { it.copy(tafsirState = com.example.mushaf.presentation.state.TafsirState.Loading(surah, ayah)) }
        viewModelScope.launch {
            val result = getTafsirForAyah(surah, ayah, tafsirKey = tafsirKey)
            when (result) {
                is Result.Success -> {
                    val tafsir = result.data
                    if (tafsir != null) {
                        _state.update { it.copy(tafsirState = com.example.mushaf.presentation.state.TafsirState.Success(tafsir)) }
                    } else {
                        _state.update {
                            it.copy(tafsirState = com.example.mushaf.presentation.state.TafsirState.Error(com.example.designsystem.text.UiText.Dynamic("Tafsir not found")))
                        }
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Error loading tafsir: ${result.error}")
                    _state.update {
                        it.copy(tafsirState = com.example.mushaf.presentation.state.TafsirState.Error(result.error.toUiText()))
                    }
                }
            }
        }
    }

    private fun fetchAvailableTafsirBooks() {
        observeAvailableTafsirBooks().onEach { books ->
            _state.update { it.copy(availableTafsirBooks = books) }
        }.catch { e ->
            Log.w(TAG, "Failed to observe available tafsir books", e)
        }.launchIn(viewModelScope)
    }

    private fun showAyahActions(surah: Int, ayah: Int) {
        noteJob?.cancel()
        _state.update {
            it.copy(
                ayahActionSheet = com.example.mushaf.presentation.state.AyahActionSheetState(
                    surahNumber = surah,
                    ayahNumber = ayah,
                ),
                ayahNoteEditorOpen = false,
            )
        }

        observeAyahNote(surah, ayah)
            .onEach { note ->
                _state.update { state ->
                    val sheet = state.ayahActionSheet
                    if (sheet != null && sheet.surahNumber == surah && sheet.ayahNumber == ayah) {
                        state.copy(ayahActionSheet = sheet.copy(note = note))
                    } else {
                        state
                    }
                }
            }
            .launchIn(viewModelScope)
            .also { noteJob = it }

        viewModelScope.launch {
            val text = getAyahText(surah, ayah).getOrNull().orEmpty()
            _state.update { state ->
                val sheet = state.ayahActionSheet
                if (sheet != null && sheet.surahNumber == surah && sheet.ayahNumber == ayah) {
                    state.copy(ayahActionSheet = sheet.copy(ayahText = text))
                } else {
                    state
                }
            }
        }
    }

    private fun dismissAyahActions() {
        noteJob?.cancel()
        noteJob = null
        _state.update { it.copy(ayahActionSheet = null, ayahNoteEditorOpen = false) }
    }

    private fun openAyahNoteEditor() {
        _state.update { it.copy(ayahNoteEditorOpen = true) }
    }

    private fun saveAyahNote(text: String) {
        val sheet = _state.value.ayahActionSheet ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { upsertAyahNote(sheet.surahNumber, sheet.ayahNumber, trimmed) }
                .onFailure { Log.e(TAG, "Failed to save ayah note", it) }
            _state.update { it.copy(ayahNoteEditorOpen = false) }
        }
    }

    private fun removeAyahNote() {
        val sheet = _state.value.ayahActionSheet ?: return
        viewModelScope.launch {
            runCatching { deleteAyahNote(sheet.surahNumber, sheet.ayahNumber) }
                .onFailure { Log.e(TAG, "Failed to delete ayah note", it) }
            _state.update { it.copy(ayahNoteEditorOpen = false) }
        }
    }

    private fun copyAyah() {
        _effects.trySend(MushafEffect.ShowMessage(R.string.ayah_copied_to_clipboard))
    }

    private fun loadReciters() {
        getReciters().onEach { result ->
            if (result is Result.Success) {
                _state.update {
                    it.copy(
                        availableReciters = result.data,
                        currentReciter = it.currentReciter ?: result.data.firstOrNull()
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun selectReciter(reciter: Reciter) {
        val wasPlaying = _state.value.audioState == AudioState.PLAYING
        val currentTrack = playbackManager.currentTrackIndex.value
        _state.update { it.copy(currentReciter = reciter) }
        if (_state.value.mushafMode == MushafMode.LISTEN && _state.value.isFollowAlongActive) {
            startFollowAlong(targetTrackIndex = currentTrack, autoPlay = wasPlaying)
        }
    }

    private fun playPauseAudio() {
        val state = _state.value
        if (state.audioState == AudioState.PLAYING) {
            playbackManager.pause()
        } else {
            if (!state.isFollowAlongActive || state.playingPage != state.currentPage) {
                startFollowAlong()
            } else {
                playbackManager.play()
            }
        }
    }

    private fun loadPage(page: Int) {
        val clamped = MushafConstants.clampPage(page)
        Log.d(TAG, "loadPage(requested=$page, clamped=$clamped)")
        val wasLive = _state.value.liveCorrection.isActive
        _state.update {
            val samePage = clamped == it.currentPage
            it.copy(
                currentPage = clamped,



                highlightedWordId = if (wasLive) it.highlightedWordId else null,
                confirmedWordId = if (wasLive) it.confirmedWordId else null,
                predictedWordId = if (wasLive) it.predictedWordId else null,
                // Only a real page change re-hides the text; a redundant load of the page we are
                // already on must not wipe what the reader has revealed.
                revealedWordIds = if (samePage) it.revealedWordIds else emptySet(),
            )
        }
        requestPage(clamped)
        requestPage(clamped - 1)
        requestPage(clamped + 1)
        requestPage(clamped - 2)
        requestPage(clamped + 2)



        if (wasLive) {
            seedInitialHighlightForCurrentPage()
            startCursorForCurrentPage()?.let(::seekLiveCorrection) ?: run { seekOnPageLoad = clamped }
        }

        viewModelScope.launch {
            runCatching { saveLastPage(clamped) }
                .onFailure { Log.e(TAG, "Failed to persist last page $clamped", it) }
        }
    }

    private fun requestPage(page: Int) {
        if (page < MushafConstants.FIRST_PAGE || page > MushafConstants.LAST_PAGE) return
        if (_state.value.pages.containsKey(page)) return
        if (loadJobs[page]?.isActive == true) return

        _state.update { it.copy(failedPages = it.failedPages - page) }
        loadJobs[page] = getPage(page)
            .catch { throwable ->
                Log.e(TAG, "Failed to load page $page", throwable)
                _state.update { it.copy(failedPages = it.failedPages + page) }
            }
            .onEach { result ->
                val loaded = result.getOrNull()
                if (loaded == null) {
                    Log.e(TAG, "Failed to load page $page: $result")
                    _state.update { it.copy(failedPages = it.failedPages + page) }
                    return@onEach
                }
                Log.d(TAG, "Page $page loaded with ${loaded.lines.size} lines")
                _state.update {
                    it.copy(
                        pages = it.pages + (page to loaded),
                        failedPages = it.failedPages - page,
                    )
                }

                if (page == _state.value.currentPage && _state.value.isRecordingActive) {
                    seedInitialHighlightForCurrentPage()
                }

                if (seekOnPageLoad == page && _state.value.liveCorrection.isActive) {
                    seekOnPageLoad = null
                    startCursorForCurrentPage()?.let(::seekLiveCorrection)
                }


            }
            .launchIn(viewModelScope)
    }

    private fun toggleTajweed(enabled: Boolean) {
        Log.d(TAG, "toggleTajweed(enabled=$enabled)")
        _state.update { it.copy(isTajweedEnabled = enabled) }
        viewModelScope.launch {
            runCatching { setTajweedEnabled(enabled) }
                .onFailure { Log.e(TAG, "Failed to persist tajweed=$enabled", it) }
        }
    }

    private fun setMode(mode: MushafMode) {
        val current = _state.value

        if (current.isOffline && (mode == MushafMode.MUALLEM || mode == MushafMode.RECITATION)) {
            viewModelScope.launch {
                _effects.send(MushafEffect.ShowMessage(R.string.mushaf_offline_mode_not_available))
            }
            return
        }

        if (current.isFollowAlongActive) stopFollowAlong()
        if (current.muallemSession != null) stopMuallemSession()
        if (current.isRecordingActive) {


            clearLiveSession()
            _state.update { it.copy(isRecordingActive = false) }
        }

        _state.update {
            it.copy(
                mushafMode = mode,
                highlightedWordId = null,
                confirmedWordId = null,
                predictedWordId = null,
                revealedWordIds = emptySet(),
                captureError = null,
            )
        }

        when (mode) {
            MushafMode.LISTEN -> { /* Audio starts when user taps play */ }
            MushafMode.MUALLEM -> {
                // Stop any active muallem session and show the setup dialog
                stopMuallemSession()
                _state.update { it.copy(showMuallemSetup = true) }
            }
            else -> Unit
        }
    }

    private fun toggleAyahVisibility() {
        val wasVisible = _state.value.areAyahsVisible
        _state.update {
            it.copy(
                areAyahsVisible = !wasVisible,
                revealedWordIds = emptySet(),
                highlightedWordId = null,
                confirmedWordId = null,
                predictedWordId = null,
            )
        }
    }

    private fun revealNextWord() {
        val state = _state.value
        if (state.areAyahsVisible) return
        val words = state.wordsForCurrentPage()
        if (words.isEmpty()) return

        val nextWord = words.firstOrNull { it.id !in state.revealedWordIds } ?: return
        _state.update {
            it.copy(
                revealedWordIds = it.revealedWordIds + nextWord.id,
                highlightedWordId = nextWord.id,
            )
        }
    }

    private fun revealNextAyah() {
        val state = _state.value
        if (state.areAyahsVisible) return
        val words = state.wordsForCurrentPage()
        if (words.isEmpty()) return

        val unrevealed = words.filter { it.id !in state.revealedWordIds }
        if (unrevealed.isEmpty()) return

        val ayahEndIndex = unrevealed.indexOfFirst { it.isEndOfAyah }
        val wordsToReveal = if (ayahEndIndex >= 0) {
            unrevealed.take(ayahEndIndex + 1)
        } else {
            unrevealed
        }

        val newRevealed = state.revealedWordIds + wordsToReveal.map { it.id }.toSet()
        _state.update {
            it.copy(
                revealedWordIds = newRevealed,
                highlightedWordId = wordsToReveal.lastOrNull()?.id,
            )
        }
    }

    private fun toggleRecording() {
        val state = _state.value
        if (state.mushafMode != MushafMode.RECITATION && state.mushafMode != MushafMode.MUALLEM) return

        // The Mu'allem session owns the mic — [startMuallemRepeat] opens it when the sheikh's
        // recitation ends and [finishMuallemRepeat] closes it. Honouring a manual toggle outside
        // the user's turn would flip isRecordingActive underneath the session and start a capture
        // the session never tears down, so the flow is guarded here and not only in the UI.
        if (state.mushafMode == MushafMode.MUALLEM &&
            state.muallemSession?.phase !is MuallemPhase.UserRecording
        ) {
            Log.d(TAG, "Ignoring mic toggle: Mu'allem phase is ${state.muallemSession?.phase}")
            return
        }

        val nowRecording = !state.isRecordingActive
        _state.update { it.copy(isRecordingActive = nowRecording, captureError = null) }

        if (nowRecording) {


            if (state.mushafMode == MushafMode.RECITATION) {
                startLiveCorrection()
            } else {
                state.page?.let { highlightDriver.start(it) }
            }
        } else {
            finishLiveCorrection()
            highlightDriver.stop()
        }
    }

    private fun startLiveCorrection(resumeAt: RecitationCursor? = null) {
        sessionJob?.cancel()
        isFinishing = false
        reconnectAttempts = 0
        beginServerSession(resumeAt ?: startCursorForCurrentPage())
    }

    private fun beginServerSession(startCursor: RecitationCursor?) {
        lastCursor = startCursor
        localCursorTracker.reset()
        sessionStartedAtEpochMs = System.currentTimeMillis()
        seedInitialHighlightForCurrentPage()

        _state.update {
            it.copy(
                captureError = null,
                liveCorrection = LiveCorrectionUiState(isConnecting = true),
            )
        }

        val controls = Channel<RecitationControl>(Channel.BUFFERED)
        controlChannel = controls
        sessionJob = viewModelScope.launch { runSession(controls) }
    }








    private suspend fun runSession(controls: Channel<RecitationControl>) {
        while (currentCoroutineContext().isActive) {
            try {
                val config = recitationSettings.toConfig(lastCursor)

                // NEW: Log the request configuration sent to the API
                Log.d(TAG, "API REQUEST (Standard) - Starting live recitation | Engine: ${config.engine} | Strictness: ${config.strictness} | Cursor: ${lastCursor?.wordId}")

                startLiveRecitation(
                    config = config,
                    controls = controls.receiveAsFlow(),
                ).collect { result ->
                    when (result) {
                        is Result.Success -> onLiveEvent(result.data)
                        // The repository already caught whatever failed; re-throw the original
                        // exception (when carried) so the catch clauses below keep dispatching on
                        // its real type (e.g. SecurityException for a lost mic permission).
                        is Result.Error -> throw (result.error as? com.iti.domain.core.DomainError.Unknown)?.exception
                            ?: (result.error as? com.iti.domain.core.DomainError.NetworkError)?.exception
                            ?: IllegalStateException("Live recitation failed: ${result.error}")
                    }
                }
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (securityError: SecurityException) {
                Log.e(TAG, "Live correction lost microphone permission", securityError)
                failCapture(CaptureError.PERMISSION_DENIED)
                return
            } catch (throwable: Throwable) {
                if (isFinishing || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                    Log.e(TAG, "Live correction session ended (finishing=$isFinishing)", throwable)
                    if (!isFinishing) failCapture(CaptureError.SERVICE_UNREACHABLE)
                    return
                }
                reconnectAttempts++
                Log.w(
                    TAG,
                    "Live session dropped; reconnect $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS " +
                        "from ${lastCursor?.wordId}",
                    throwable,
                )
                _state.update {
                    it.copy(liveCorrection = it.liveCorrection.copy(isConnecting = true, isActive = false))
                }
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    private fun onLiveEvent(event: LiveRecitationEvent) {
        when (event) {
            is LiveRecitationEvent.Started -> {
                reconnectAttempts = 0
                Log.i(TAG, "Live session ${event.sessionId} on engine '${event.engine}'")
                _state.update {
                    it.copy(
                        liveCorrection = it.liveCorrection.copy(
                            isConnecting = false,
                            isActive = true,
                            engine = event.engine,
                            engineSubstituted = event.engineSubstituted,
                        ),
                    )
                }
            }

            is LiveRecitationEvent.Level -> updateMicLevel(event)

            is LiveRecitationEvent.LocalPhonemes -> updateLocalCursor(event.transcript)

            is LiveRecitationEvent.Graded -> mergeChunk(event.chunk)

            LiveRecitationEvent.Finished -> {
                Log.d(TAG, "Live session finished")

                val restart = pendingRestart
                pendingRestart = null
                val restarting = restart != null

                recordFinishedSession(showSummary = restart != SessionRestart.SETTINGS)

                val resumeAt = lastCursor.takeIf { restart == SessionRestart.SETTINGS }

                _state.update {
                    it.copy(


                        isRecordingActive = restarting,
                        micLevel = 0f,
                        isSpeechDetected = false,
                        highlightedWordId = null,
                        confirmedWordId = null,
                        predictedWordId = null,





                        liveCorrection = LiveCorrectionUiState(),
                    )
                }
                if (restarting) {


                    sessionJob = null
                    viewModelScope.launch { startLiveCorrection(resumeAt) }
                }
            }
        }
    }

    private fun updateMicLevel(event: LiveRecitationEvent.Level) {
        smoothedMicLevel = if (event.isSpeaking) {
            smoothedMicLevel + (event.amplitude * MIC_LEVEL_GAIN - smoothedMicLevel) * MIC_LEVEL_SMOOTHING
        } else {
            0f
        }

        // No timing-based guess here on purpose: highlightedWordId only ever moves once a word
        // is actually confirmed via mergeChunk's server cursor, never from a blind frame-count
        // estimate. See docs/features/06-taahud-live-highlight-sync-code-audit.md.
        _state.update {
            it.copy(
                micLevel = smoothedMicLevel.coerceIn(0f, 1f),
                isSpeechDetected = event.isSpeaking,
            )
        }
    }

    /**
     * The on-device model's running phoneme transcript (see [LiveRecitationEvent.LocalPhonemes]),
     * arriving roughly every 100ms.
     *
     * This is the *only* thing that moves the highlight between server chunks, and it is
     * deliberately narrow in what it may do: it advances [MushafUiState.predictedWordId], and
     * nothing else. It does not reveal words under the memorisation veil (an unconfirmed guess
     * must not uncover text the reciter may not have reached), does not turn the page, and never
     * feeds grading. It also only ever moves *forward* — a prediction that lands behind where the
     * highlight already is means the aligner is out of step, and the server cursor will resolve it
     * within a chunk or two.
     */
    private fun updateLocalCursor(transcript: LocalTranscript) {
        val predicted = localCursorTracker.offer(transcript.phonemes) ?: return
        val state = _state.value
        if (!state.isRecordingActive) return
        if (!RecitationWordOrder.isAfter(predicted, state.highlightedWordId)) return

        Log.d(TAG, "Local cursor -> $predicted (confirmed: ${state.confirmedWordId})")
        _state.update { it.copy(highlightedWordId = predicted, predictedWordId = predicted) }
    }

    private fun mergeChunk(chunk: RecitationChunk) {
        chunk.cursor?.let { lastCursor = it }

        // The server cursor is authoritative - slide the local tracker's window to a fresh small
        // span starting just past it, so local tracking never drifts onto a large, stale span
        // after a correction (see LOCAL_TRACKER_WINDOW_WORDS for why the window must stay small).
        chunk.cursor?.let { seedLocalTracker(it.copy(wordIndex = it.wordIndex + 1)) }

        if (chunk.mistakeWords.isEmpty()) {
            Log.d(TAG, "API RESPONSE (Standard) - NO MISTAKES \u2705 | Words: ${chunk.words.joinToString { it.wordId }} | Cursor: ${chunk.cursor?.wordId}")
        } else {
            Log.d(TAG, "API RESPONSE (Standard) - MISTAKES DETECTED \u274C | Mistakes: ${chunk.mistakeWords.joinToString { it.wordId }} | All words: ${chunk.words.joinToString { it.wordId }} | Cursor: ${chunk.cursor?.wordId}")
        }
        advancePageIfRecitationMovedOn(chunk.cursor)

        _state.update { state ->
            val live = state.liveCorrection
            val confirmed = chunk.cursor?.wordId ?: state.confirmedWordId
            // The prediction survives only while it is still ahead of the server. Once the server
            // catches up to or passes it, it was either right (and adds nothing) or wrong (and
            // must go), so either way the confirmed cursor takes over and the prediction restarts
            // from there.
            val predictionStillLeads = RecitationWordOrder.isAfter(state.predictedWordId, confirmed)
            val predicted = if (predictionStillLeads) state.predictedWordId else confirmed
            state.copy(
                highlightedWordId = predicted ?: state.highlightedWordId,
                confirmedWordId = confirmed,
                predictedWordId = predicted,
                liveCorrection = live.copy(
                    wordFeedback = live.wordFeedback.mergedWith(chunk),
                    candidates = (chunk.match as? RecitationMatch.Ambiguous)?.candidates.orEmpty(),
                    nonVerse = chunk.nonVerse,
                    lastOutcome = chunk.match.toOutcome(),
                    cursor = chunk.cursor ?: live.cursor,
                ),
            ).revealing(chunk.cursor?.wordId)
        }
    }


    private fun advancePageIfRecitationMovedOn(cursor: RecitationCursor?) {
        val wordId = cursor?.wordId ?: return
        val state = _state.value
        if (!state.isRecordingActive) return
        if (state.wordsForCurrentPage().any { it.id == wordId }) return

        val nextPage = state.currentPage + 1
        val landsOnNextPage = state.pages[nextPage]
            ?.lines
            ?.any { line -> line.words.any { it.id == wordId } } == true

        if (landsOnNextPage) {
            Log.d(TAG, "Recitation crossed onto page $nextPage; following")
            loadPage(nextPage)
        }
    }


    private fun seedInitialHighlightForCurrentPage() {
        val pageWords = _state.value.wordsForCurrentPage()
        val anchor = lastCursor?.takeIf { cursor -> pageWords.any { it.id == cursor.wordId } }
            ?: startCursorForCurrentPage()
            ?: return
        _state.update {
            it.copy(
                highlightedWordId = anchor.wordId,
                confirmedWordId = anchor.wordId,
                predictedWordId = anchor.wordId,
            )
        }
        seedLocalTracker(anchor)
    }


    /**
     * Re-anchors the on-device tracker on the phonemes expected from [firstExpected] onwards.
     *
     * Callers pass the first word the reciter has *not* said yet, which is the seed cursor itself
     * when a session or page starts, but the word *after* a graded chunk's cursor — that one
     * reports the last word already recited, and a window that began there would have the tracker
     * waiting to hear something that is over.
     */
    private fun seedLocalTracker(firstExpected: RecitationCursor) {
        viewModelScope.launch {
            val window = referencePhonemeRepository
                .unitsFrom(firstExpected, LOCAL_TRACKER_WINDOW_WORDS)
                .getOrNull()
            Log.d(TAG, "seedLocalTracker(${firstExpected.wordId}): units=${window?.size ?: "FETCH FAILED"}")
            if (window == null) return@launch
            localCursorTracker.setWindow(window)
        }
    }

    private fun selectCandidate(position: RecitationCursor) {
        seekLiveCorrection(position)
        clearCandidates()
    }

    private fun clearCandidates() = _state.update {
        it.copy(liveCorrection = it.liveCorrection.copy(candidates = emptyList()))
    }

    private fun finishAndStartNewSession() {
        val state = _state.value
        if (!state.isRecordingActive || state.mushafMode != MushafMode.RECITATION) return
        pendingRestart = SessionRestart.BOUNDARY
        finishLiveCorrection()
    }










    /**
     * The on-page practice-mode toggle. A shortcut into the same stored setting the engine picker
     * in Recite Settings writes, not a second setting: [RecitationSettings.withTajweedGrading]
     * moves the engine with it, so "memorisation only" actually switches to the follow-along
     * engine instead of leaving the tajwīd engine running with its rules muted.
     */
    private fun setTajweedGrading(enabled: Boolean) {
        val current = recitationSettings
        if (current.gradesTajweed == enabled && current.engineCanGradeTajweed == enabled) return

        val updated = current.withTajweedGrading(enabled)

        recitationSettings = updated
        _state.update { it.copy(isTajweedGradingEnabled = enabled) }

        viewModelScope.launch {
            runCatching { updateRecitationSettings(updated) }
                .onFailure { Log.e(TAG, "Failed to store the tajwid grading choice", it) }
        }

        val state = _state.value
        if (state.isRecordingActive &&
            state.mushafMode == MushafMode.RECITATION &&
            state.liveCorrection.isActive
        ) {
            Log.d(TAG, "Practice mode -> ${updated.wireEngine}; reopening the session at ${lastCursor?.wordId}")
            pendingRestart = SessionRestart.SETTINGS
            finishLiveCorrection()
        }
    }

    private fun recordFinishedSession(showSummary: Boolean = true) {
        if (sessionStartedAtEpochMs == 0L) return
        val live = _state.value.liveCorrection

        val summary = RecitationSessionRecorder.record(
            id = UUID.randomUUID().toString(),
            startedAtEpochMs = sessionStartedAtEpochMs,
            durationMs = (System.currentTimeMillis() - sessionStartedAtEpochMs).coerceAtLeast(0L),
            wordFeedback = live.wordFeedback,
            fallbackPosition = live.cursor ?: lastCursor,
        )

        sessionStartedAtEpochMs = 0L
        if (showSummary) _state.update { it.copy(sessionSummary = summary) }
        viewModelScope.launch {
            runCatching { saveRecitationSession(summary) }
                .onFailure { Log.e(TAG, "Failed to save the session record", it) }
        }
    }

    private fun RecitationMatch.toOutcome(): ChunkOutcome = when (this) {
        is RecitationMatch.Matched -> ChunkOutcome.GRADED
        is RecitationMatch.Ambiguous -> ChunkOutcome.AMBIGUOUS
        RecitationMatch.NoMatch -> ChunkOutcome.NO_MATCH
    }







    private fun finishLiveCorrection() {
        isFinishing = true
        val controls = controlChannel
        if (controls == null) {
            clearLiveSession()
            return
        }

        // NEW: Log the Finish request
        Log.d(TAG, "API REQUEST CONTROL - Sending FINISH control to API")

        if (controls.trySend(RecitationControl.Finish).isFailure) {
            Log.w(TAG, "Could not request a flush; ending the session locally")
            clearLiveSession()
        }
        _state.update { it.copy(micLevel = 0f, isSpeechDetected = false) }
    }


    private fun clearLiveSession() {
        isFinishing = true
        sessionJob?.cancel()
        sessionJob = null
        controlChannel?.close()
        controlChannel = null
        smoothedMicLevel = 0f
        localCursorTracker.reset()
        sessionStartedAtEpochMs = 0L
        pendingRestart = null
        _state.update {
            it.copy(
                micLevel = 0f,
                isSpeechDetected = false,
                highlightedWordId = null,
                confirmedWordId = null,
                predictedWordId = null,
                liveCorrection = LiveCorrectionUiState(),
            )
        }
    }


    private fun seekLiveCorrection(cursor: RecitationCursor) {
        lastCursor = cursor
        // NEW: Log the Seek request
        Log.d(TAG, "API REQUEST CONTROL - Sending SEEK control to: ${cursor.wordId}")
        controlChannel?.trySend(RecitationControl.Seek(cursor))
    }

    private fun startCursorForCurrentPage(): RecitationCursor? =
        _state.value.wordsForCurrentPage()
            .firstNotNullOfOrNull { RecitationCursor.fromWordId(it.id) }

    private fun failCapture(error: CaptureError) {
        clearLiveSession()
        _state.update { it.copy(isRecordingActive = false, captureError = error) }
    }


    fun onScreenStopped() {
        if (_state.value.isRecordingActive) {
            Log.d(TAG, "Screen stopped while recording; releasing the microphone")
            clearLiveSession()
            highlightDriver.stop()
            _state.update { it.copy(isRecordingActive = false) }
        }
    }

    private fun startFollowAlong(targetPageNumber: Int = _state.value.currentPage, targetSurahNumber: Int? = null, targetTrackIndex: Int? = null, autoPlay: Boolean = true) {
        val page = _state.value.pages[targetPageNumber] ?: return
        val reciter = _state.value.currentReciter

        Log.d(TAG, "startFollowAlong() started. Mode=${_state.value.mushafMode}, Reciter=${reciter?.name}, Page=${page.pageNumber}")


        playbackManager.stop()

        _state.update { it.copy(isFollowAlongActive = true, playingPage = page.pageNumber) }

        if (_state.value.mushafMode == MushafMode.LISTEN && reciter != null) {
            viewModelScope.launch {
                val timingsResult = fetchTimingsForPage(page.pageNumber, reciter.id)
                val timings = (timingsResult as? com.iti.domain.core.Result.Success)?.data ?: emptyList()
                if (timings.isNotEmpty()) {
                    audioHighlightDriver.loadTimings(timings)
                    val urls = buildAudioTracks(timings, reciter)

                    if (urls.isNotEmpty() && timings.isNotEmpty()) {
                        val firstSurah = timings.first().surahNumber
                        Log.d(TAG, "First surah on page: $firstSurah")
                        Log.d(TAG, "requested link is : ${urls.first().url}")
                    }

                    val startIndex = when {
                        targetTrackIndex != null -> targetTrackIndex.coerceIn(0, timings.lastIndex)
                        targetSurahNumber != null -> timings.indexOfFirst { it.surahNumber == targetSurahNumber }.coerceAtLeast(0)
                        else -> 0
                    }

                    Log.d(TAG, "Playing ${urls.size} audio URLs for this page, startIndex=$startIndex")
                    playbackManager.playTracks(urls, startIndex, autoPlay)
                    audioHighlightDriver.start(page)
                } else {
                    Log.w(TAG, "No timings were loaded. Cannot play audio.")
                }
            }
        } else {
            Log.d(TAG, "Starting simulated highlight driver instead of audio.")
            simulatedHighlightDriver.start(page)
        }
    }

    private suspend fun fetchTimingsForPage(pageNumber: Int, reciterId: Int): com.iti.domain.core.Result<List<AyahTiming>> {
        return try {
            val result = getAyahTimings(reciterId, pageNumber).first()
            if (result is com.iti.domain.core.Result.Success) {
                Log.d(TAG, "Loaded ${result.data.size} timings for page $pageNumber")
            } else {
                Log.e(TAG, "getAyahTimings returned non-success for page $pageNumber: $result")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ayah timings for page $pageNumber", e)
            com.iti.domain.core.Result.Error(com.iti.domain.core.DomainError.NetworkError(e))
        }
    }

    private suspend fun buildAudioTracks(timings: List<AyahTiming>, reciter: Reciter): List<AudioPlayer.AudioTrackInfo> {
        val isArabic = observeAppPreferences().first().language == com.iti.domain.settings.model.AppLanguage.ARABIC
        return timings.map { timing ->
            val audioUrl = timing.audioUrl
            val finalUrl = when {
                audioUrl.isNullOrBlank() -> {
                    val paddedS = timing.surahNumber.toString().padStart(3, '0')
                    val paddedA = timing.ayahNumber.toString().padStart(3, '0')
                    "${reciter.audioBaseUrl}${paddedS}${paddedA}.mp3"
                }
                java.io.File(audioUrl).exists() -> {
                    java.io.File(audioUrl).toURI().toString()
                }
                audioUrl.startsWith("http://") || audioUrl.startsWith("https://") || audioUrl.startsWith("file://") || audioUrl.startsWith("content://") -> {
                    audioUrl
                }
                audioUrl.startsWith("//") -> {
                    "https:$audioUrl"
                }
                else -> {
                    "https://audio.qurancdn.com/${audioUrl.removePrefix("/")}"
                }
            }
            
            val surah = com.example.mushaf.domain.model.SurahCatalog.all.getOrNull(timing.surahNumber - 1)
            val surahName = if (isArabic) surah?.nameArabic ?: "Surah ${timing.surahNumber}" else surah?.nameEnglish ?: "Surah ${timing.surahNumber}"
            val ayahLabel = if (isArabic) "آية" else "Ayah"
            val reciterName = if (isArabic) reciter.nameArabic else reciter.name
            
            AudioPlayer.AudioTrackInfo(
                title = "$surahName - $ayahLabel ${timing.ayahNumber}",
                artist = reciterName,
                url = finalUrl
            )
        }
    }

    private fun stopFollowAlong() {
        highlightDriver.stop()
        if (_state.value.mushafMode == MushafMode.LISTEN) {
            playbackManager.stop()
        }
        _state.update { it.copy(isFollowAlongActive = false, playingPage = null, highlightedWordId = null) }
    }

    // ── Mu'allem session ────────────────────────────────────────────────────────

    private fun startMuallemSession(surah: Int, startAyah: Int, endAyah: Int, repeatCount: Int, difficulty: com.example.mushaf.domain.model.recite.RecitationStrictness) {
        _state.update { it.copy(showMuallemSetup = false) }
        viewModelScope.launch {
            val pageResult = getTargetPage.forAyah(surah, startAyah)
            val page = (pageResult as? Result.Success)?.data
            if (page != null) {
                loadPage(page)
            }
            muallemStartedAtEpochMs = System.currentTimeMillis()
            _state.update {
                it.copy(
                    muallemSession = MuallemSessionState(
                        surah = surah,
                        currentAyah = startAyah,
                        endAyah = endAyah,
                        difficulty = difficulty,
                        repeatCount = repeatCount,
                        currentRepeat = 0,
                        phase = MuallemPhase.SheikhPlaying,
                    ),
                    liveCorrection = LiveCorrectionUiState(),
                    isRecordingActive = false,
                )
            }
            playMuallemSheikhAyah()
        }
    }

    private fun stopMuallemSession(showSummaryIfAny: Boolean = false) {
        muallemAutoAdvanceJob?.cancel()
        muallemAutoAdvanceJob = null
        muallemControlChannel?.trySend(RecitationControl.Finish)
        muallemSessionJob?.cancel()
        muallemSessionJob = null
        muallemControlChannel = null
        muallemAyahWordCount = null
        muallemRepeatFinishing = false

        if (showSummaryIfAny) {
            val session = _state.value.muallemSession
            if (session != null && (session.repeatFeedbacks.isNotEmpty() || session.accumulatedFeedbacks.isNotEmpty())) {
                recordMuallemSession(showSummary = true)
                playbackManager.stop()
                audioHighlightDriver.stop()
                return
            }
        }

        _state.update {
            it.copy(
                muallemSession = null,
                isRecordingActive = false,
                liveCorrection = LiveCorrectionUiState(),
                highlightedWordId = null,
            )
        }
        playbackManager.stop()
        audioHighlightDriver.stop()
    }

    private fun playMuallemSheikhAyah() {
        val session = _state.value.muallemSession ?: return
        val reciter = _state.value.currentReciter ?: return

        _state.update {
            it.copy(
                muallemSession = session.copy(phase = MuallemPhase.SheikhPlaying),
                liveCorrection = LiveCorrectionUiState(),       // clear last repeat's marks
                isRecordingActive = false,
                highlightedWordId = null,
            )
        }

        viewModelScope.launch {
            // Get the page for this ayah
            val pageResult = getTargetPage.forAyah(session.surah, session.currentAyah)
            val pageNumber = (pageResult as? Result.Success)?.data ?: return@launch

            if (pageNumber != _state.value.currentPage) {
                loadPage(pageNumber)
            } else {
                ensureMuallemAyahPageLoaded(session.surah, session.currentAyah)
            }

            // Fetch timings for this page
            val timingsResult = fetchTimingsForPage(pageNumber, reciter.id)
            if (timingsResult is com.iti.domain.core.Result.Error) {
                Log.e(TAG, "Mu'allem: Failed to fetch timings. Stopping session.")
                failCapture(CaptureError.SERVICE_UNREACHABLE)
                stopMuallemSession()
                return@launch
            }
            val timings = (timingsResult as? com.iti.domain.core.Result.Success)?.data ?: emptyList()
            val timing = timings.firstOrNull {
                it.surahNumber == session.surah && it.ayahNumber == session.currentAyah
            }

            if (timing?.audioUrl != null) {
                val raw = timing.audioUrl!!
                val url = when {
                    raw.startsWith("http") -> raw
                    raw.startsWith("//") -> "https:$raw"
                    else -> "https://audio.qurancdn.com/${raw.removePrefix("/")}"
                }
                audioHighlightDriver.loadTimings(listOf(timing))
                playbackManager.playTracks(
                    tracks = listOf(
                        AudioPlayer.AudioTrackInfo(
                            url = url,
                            title = "Surah ${session.surah} : ${session.currentAyah}",
                            artist = reciter.nameArabic,
                        )
                    ),
                    startIndex = 0,
                    autoPlay = true,
                )
                val pageData = _state.value.pages[pageNumber]
                if (pageData != null) {
                    audioHighlightDriver.start(pageData)
                }
            } else {
                // Fallback: no audio URL — skip sheikh, go straight to user recording
                Log.w(TAG, "Mu'allem: No audio URL for ${session.surah}:${session.currentAyah}, skipping sheikh")
                startMuallemRepeat()
            }
        }
    }

    /** Called when sheikh audio ends (observed in the audioState flow). */
    private fun startMuallemRepeat() {
        val session = _state.value.muallemSession ?: return
        val nextRepeat = session.currentRepeat + 1
        muallemRepeatFinishing = false

        _state.update {
            it.copy(
                muallemSession = session.copy(
                    currentRepeat = nextRepeat,
                    phase = MuallemPhase.UserRecording(nextRepeat),
                ),
                isRecordingActive = true,
                liveCorrection = LiveCorrectionUiState(isConnecting = true),
                highlightedWordId = null,
            )
        }

        muallemControlChannel?.trySend(RecitationControl.Finish)
        muallemSessionJob?.cancel()

        val controls = Channel<RecitationControl>(Channel.BUFFERED)
        muallemControlChannel = controls

        muallemSessionJob = viewModelScope.launch {
            try {
                ensureMuallemAyahPageLoaded(session.surah, session.currentAyah)
                val pageNumber = (getTargetPage.forAyah(session.surah, session.currentAyah) as? Result.Success)?.data
                if (pageNumber != null && pageNumber != _state.value.currentPage) {
                    loadPage(pageNumber)
                }
                muallemAyahWordCount = resolveMuallemAyahWordCount(session)

                val cursor = RecitationCursor(session.surah, session.currentAyah, 0)
                val config = recitationSettings.toConfig(cursor).copy(
                    engine = "real",
                    strictness = session.difficulty,
                )

                // NEW: Log the Mu'allem request configuration
                Log.d(TAG, "API REQUEST (Muallem) - Starting live recitation | Engine: ${config.engine} | Strictness: ${config.strictness} | Cursor: ${cursor.wordId}")
                startLiveRecitation(config, controls.receiveAsFlow()).collect { result ->
                    when (result) {
                        is Result.Success -> onMuallemLiveEvent(result.data)
                        is Result.Error -> {
                            val error = (result.error as? com.iti.domain.core.DomainError.Unknown)?.exception
                                ?: (result.error as? com.iti.domain.core.DomainError.NetworkError)?.exception
                            if (error is SecurityException) {
                                failCapture(CaptureError.PERMISSION_DENIED)
                                stopMuallemSession()
                            } else {
                                Log.e(TAG, "Mu'allem live session error: ${result.error}", error)
                                failCapture(CaptureError.SERVICE_UNREACHABLE)
                                stopMuallemSession()
                            }
                        }
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                Log.e(TAG, "Mu'allem live session error", e)
                failCapture(CaptureError.SERVICE_UNREACHABLE)
                stopMuallemSession()
            }
        }
    }

    private fun finishMuallemRepeat() {
        if (muallemRepeatFinishing) return
        val session = _state.value.muallemSession ?: return
        if (session.phase !is MuallemPhase.UserRecording) return

        muallemRepeatFinishing = true
        Log.d(TAG, "Mu'allem: finishing repeat ${session.currentRepeat}")

        // 1. Send the finish signal to the engine to close it gracefully on the backend
        muallemControlChannel?.trySend(RecitationControl.Finish)

        // 2. IMMEDIATELY tear down the session locally instead of waiting for
        // LiveRecitationEvent.Finished. This forces the mic to close instantly,
        // preventing the user from spilling into the next ayah.
        muallemSessionJob?.cancel()
        muallemSessionJob = null
        muallemControlChannel?.close()
        muallemControlChannel = null

        // 3. Reset the finishing flag and manually trigger the feedback transition
        muallemRepeatFinishing = false
        onMuallemRepeatFinished()
    }

    private suspend fun ensureMuallemAyahPageLoaded(surah: Int, ayah: Int) {
        val pageNumber = (getTargetPage.forAyah(surah, ayah) as? Result.Success)?.data ?: return
        if (_state.value.pages.containsKey(pageNumber)) return
        getPage(pageNumber).first { it.getOrNull() != null || it is Result.Error }
            .getOrNull()?.let { loaded ->
                _state.update { it.copy(pages = it.pages + (pageNumber to loaded)) }
            }
    }

    private suspend fun resolveMuallemAyahWordCount(session: MuallemSessionState): Int? {
        ayahWordCountFromPages(session)?.let { return it }
        val words = localWordCorpusRepository
            .wordsForAyah(session.surah, session.currentAyah)
            .getOrNull()
            .orEmpty()
        return words.size.takeIf { it > 0 }
    }

    private fun ayahWordCountFromPages(session: MuallemSessionState): Int? {
        val prefix = "${session.surah}:${session.currentAyah}:"
        val lastIndex = _state.value.pages.values
            .asSequence()
            .flatMap { page -> page.lines.asSequence().flatMap { it.words.asSequence() } }
            .filter { word -> word.id.startsWith(prefix) && !word.isEndOfAyah }
            .mapNotNull { word -> RecitationCursor.fromWordId(word.id)?.wordIndex }
            .maxOrNull()
        return lastIndex?.plus(1)
    }

    private fun isAtOrPastMuallemAyahEnd(cursor: RecitationCursor, session: MuallemSessionState): Boolean {
        if (cursor.sura > session.surah) return true
        if (cursor.sura < session.surah) return false
        if (cursor.aya > session.currentAyah) return true
        if (cursor.aya < session.currentAyah) return false
        val wordCount = muallemAyahWordCount ?: ayahWordCountFromPages(session) ?: return false
        return cursor.wordIndex >= wordCount - 1
    }

    private fun shouldFinishMuallemRepeat(chunk: RecitationChunk, session: MuallemSessionState): Boolean {
        if (chunk.forcedCut) return true

        chunk.cursor?.let { cursor ->
            if (isAtOrPastMuallemAyahEnd(cursor, session)) return true
        }

        val matchEnd = (chunk.match as? RecitationMatch.Matched)?.end
        matchEnd?.let { end ->
            if (isAtOrPastMuallemAyahEnd(end, session)) return true
        }

        chunk.words.lastOrNull()?.position?.let { lastPosition ->
            if (isAtOrPastMuallemAyahEnd(lastPosition, session)) return true
        }

        return false
    }

    private fun onMuallemLiveEvent(event: LiveRecitationEvent) {
        when (event) {
            is LiveRecitationEvent.Started -> {
                Log.d("MuallemLatency", "🔌 Socket Session Opened and Engine Started")
                _state.update {
                    it.copy(liveCorrection = it.liveCorrection.copy(isConnecting = false, isActive = true))
                }
            }
            is LiveRecitationEvent.Level -> {
                // Track when speech begins for latency calculations
                if (event.isSpeaking && muallemSpeechStartTime == 0L) {
                    muallemSpeechStartTime = System.currentTimeMillis()
                    Log.d("MuallemLatency", "🎙️ Request (Speech) started at: $muallemSpeechStartTime")
                } else if (!event.isSpeaking) {
                    muallemSpeechStartTime = 0L // Reset when user stops speaking
                }
                updateMicLevel(event)
            }
            // Mu'allem highlights only from graded chunks, same as free recitation - see the
            // matching comment in updateMicLevel.
            is LiveRecitationEvent.LocalPhonemes -> Unit
            is LiveRecitationEvent.Graded -> {
                val now = System.currentTimeMillis()
                val latency = if (muallemSpeechStartTime > 0L) now - muallemSpeechStartTime else 0L

                // NEW: Log the Mu'allem response and check for mistakes
                val chunk = event.chunk
                if (chunk.mistakeWords.isEmpty()) {
                    Log.d(TAG, "API RESPONSE (Muallem) - NO MISTAKES \u2705 | Latency: ${latency}ms | Words: ${chunk.words.joinToString { it.wordId }}")
                } else {
                    // We leave joinToString() empty so it prints the entire object (showing all mistake properties)
                    val mistakesDetail = chunk.mistakeWords.joinToString()
                    Log.d(TAG, "API RESPONSE (Muallem) - MISTAKES DETECTED \u274C | Latency: ${latency}ms | Mistakes: $mistakesDetail | All words: ${chunk.words.joinToString { it.wordId }}")
                }

                val session = _state.value.muallemSession
                // Force the engine to only accept words from the exact Ayah we are practicing
                val expectedPrefix = session?.let { "${it.surah}:${it.currentAyah}:" }

                val newFeedback = _state.value.liveCorrection.wordFeedback.toMutableMap()
                event.chunk.words.forEach { w ->
                    // Ignore any words the engine thought belonged to a different Ayah
                    if (expectedPrefix == null || w.wordId.startsWith(expectedPrefix)) {
                        newFeedback[w.wordId] = w
                    } else {
                        Log.d("MuallemLatency", "🚫 Rejected out-of-ayah word jump: ${w.wordId}")
                    }
                }

                _state.update {
                    it.copy(liveCorrection = it.liveCorrection.copy(wordFeedback = newFeedback))
                }

                // Highlight the last valid word inside our Ayah
                event.chunk.words.lastOrNull { expectedPrefix == null || it.wordId.startsWith(expectedPrefix) }?.let { lastWord ->
                    updateWordHighlight(lastWord.wordId)
                }

                if (session != null &&
                    session.phase is MuallemPhase.UserRecording &&
                    !muallemRepeatFinishing &&
                    shouldFinishMuallemRepeat(event.chunk, session)
                ) {
                    Log.d(TAG, "Mu'allem: reached end of ayah ${session.surah}:${session.currentAyah}, finishing repeat")
                    finishMuallemRepeat()
                }
            }
            LiveRecitationEvent.Finished -> {
                Log.d("MuallemLatency", "🔌 Socket Session Finished/Closed")
                muallemRepeatFinishing = false
                onMuallemRepeatFinished()
            }
        }
    }
    private fun onMuallemRepeatFinished() {
        val session = _state.value.muallemSession ?: return
        val repeatFeedback = MuallemRepeatFeedback(
            repeatIndex = session.currentRepeat,
            wordFeedback = _state.value.liveCorrection.wordFeedback,
        )
        val updatedFeedbacks = session.repeatFeedbacks + repeatFeedback
        val updatedSession = session.copy(
            repeatFeedbacks = updatedFeedbacks,
            phase = MuallemPhase.ShowingFeedback(session.currentRepeat),
        )
        _state.update {
            it.copy(
                muallemSession = updatedSession,
                isRecordingActive = false,
                micLevel = 0f,
                isSpeechDetected = false,
            )
        }

        muallemAutoAdvanceJob?.cancel()
        muallemAutoAdvanceJob = viewModelScope.launch {
            delay(3_500L)
            val s = _state.value.muallemSession ?: return@launch
            if (s.phase !is MuallemPhase.ShowingFeedback) return@launch
            if (s.currentRepeat < s.repeatCount) {
                startMuallemRepeat()
            } else {
                advanceMuallemAyah()
            }
        }
    }

    private fun advanceMuallemAyah() {
        val session = _state.value.muallemSession ?: return
        val surahInfo = SurahCatalog.all.getOrNull(session.surah - 1)
        val maxAyah = surahInfo?.verseCount ?: 286
        val nextAyah = session.currentAyah + 1

        val allFeedbacksSoFar = session.accumulatedFeedbacks + session.repeatFeedbacks

        if (nextAyah > session.endAyah || nextAyah > maxAyah) {
            // End of selected range or surah — session complete
            Log.i(TAG, "Mu'allem: reached end of surah ${session.surah} or selected range")
            recordMuallemSession(showSummary = true)
            return
        }

        val nextSession = session.copy(
            currentAyah = nextAyah,
            currentRepeat = 0,
            phase = MuallemPhase.SheikhPlaying,
            repeatFeedbacks = emptyList(),
            accumulatedFeedbacks = allFeedbacksSoFar,
        )
        muallemAyahWordCount = null
        muallemRepeatFinishing = false
        _state.update {
            it.copy(
                muallemSession = nextSession,
                liveCorrection = LiveCorrectionUiState(),
                isRecordingActive = false,
            )
        }
        playMuallemSheikhAyah()
    }

    private fun recordMuallemSession(showSummary: Boolean = true) {
        if (muallemStartedAtEpochMs == 0L) return
        val session = _state.value.muallemSession ?: return

        // Consolidate word feedback from all repetitions across all ayahs.
        // Later repetitions overwrite earlier ones, reflecting the student's final performance.
        val consolidatedFeedback = mutableMapOf<String, com.example.mushaf.domain.model.recite.RecitationWordFeedback>()
        val totalFeedbacks = session.accumulatedFeedbacks + session.repeatFeedbacks
        totalFeedbacks.forEach { repeatFeedback ->
            consolidatedFeedback.putAll(repeatFeedback.wordFeedback)
        }

        val fallbackCursor = RecitationCursor(session.surah, session.endAyah, 0)

        val summary = RecitationSessionRecorder.record(
            id = UUID.randomUUID().toString(),
            startedAtEpochMs = muallemStartedAtEpochMs,
            durationMs = (System.currentTimeMillis() - muallemStartedAtEpochMs).coerceAtLeast(0L),
            wordFeedback = consolidatedFeedback,
            fallbackPosition = fallbackCursor,
        )

        muallemStartedAtEpochMs = 0L
        _state.update {
            it.copy(
                muallemSession = null,
                isRecordingActive = false,
                liveCorrection = LiveCorrectionUiState(),
                sessionSummary = if (showSummary) summary else it.sessionSummary
            )
        }
        viewModelScope.launch {
            runCatching { saveRecitationSession(summary) }
                .onFailure { Log.e(TAG, "Failed to save the Mu'allem session record", it) }
        }
    }

    override fun onCleared() {
        clearLiveSession()
        stopMuallemSession()
        simulatedHighlightDriver.stop()
        audioHighlightDriver.stop()
        playbackManager.release()
        super.onCleared()
    }
}
