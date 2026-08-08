package com.example.mushaf.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.domain.model.MushafWord
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.domain.usecase.GetRecitersUseCase
import com.example.mushaf.domain.usecase.GetAyahTimingsUseCase
import com.example.mushaf.domain.usecase.StartLiveRecitationUseCase
import com.example.mushaf.domain.usecase.ObserveRecitationSettingsUseCase
import com.example.mushaf.domain.usecase.UpdateRecitationSettingsUseCase
import com.example.mushaf.domain.model.recite.RecitationSettings
import com.iti.domain.usecase.SaveRecitationSessionUseCase
import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationChunk
import com.example.mushaf.domain.model.recite.RecitationControl
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMatch
import com.example.mushaf.domain.model.recite.RecitationSessionRecorder
import com.example.mushaf.domain.model.recite.mergedWith
import com.example.mushaf.presentation.state.ChunkOutcome
import com.example.mushaf.presentation.state.LiveCorrectionUiState
import com.example.mushaf.presentation.state.CaptureError
import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.presentation.audio.AudioPlayer
import com.example.mushaf.presentation.audio.AudioState
import com.example.mushaf.presentation.highlight.AudioHighlightDriver
import com.example.mushaf.presentation.highlight.HighlightDriver
import com.example.mushaf.presentation.highlight.SimulatedHighlightDriver
import com.example.mushaf.presentation.state.MushafEffect
import com.iti.domain.core.Result
import com.iti.domain.core.getOrNull
import com.example.designsystem.text.UiText
import com.example.mushaf.presentation.core.error.toUiText
import com.example.mushaf.presentation.state.MushafIntent
import com.example.mushaf.presentation.state.MushafUiState
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
    private val setFirstMushafLaunchCompleted: com.example.mushaf.domain.usecase.SetFirstMushafLaunchCompletedUseCase,
    private val saveLastPage: SaveLastPageUseCase,
    private val getReciters: GetRecitersUseCase,
    private val getAyahTimings: GetAyahTimingsUseCase,
    private val getTafsirForAyah: com.example.mushaf.domain.usecase.GetTafsirForAyahUseCase,
    private val playbackManager: AudioPlayer,
    private val startLiveRecitation: StartLiveRecitationUseCase,
    private val saveRecitationSession: SaveRecitationSessionUseCase,
    private val observeRecitationSettings: ObserveRecitationSettingsUseCase,
    private val updateRecitationSettings: UpdateRecitationSettingsUseCase,
    private val downloadRecitation: com.example.mushaf.domain.usecase.DownloadRecitationUseCase,
    private val observeAvailableTafsirBooks: com.example.mushaf.domain.usecase.ObserveAvailableTafsirBooksUseCase,
    private val manageTafsirDownload: com.example.mushaf.domain.usecase.ManageTafsirDownloadUseCase,
    private val observeAppPreferences: com.iti.domain.usecase.settings.ObserveAppPreferencesUseCase,
    private val connectivityObserver: com.iti.domain.connectivity.ConnectivityObserver,
    private val toggleBookmarkUseCase: com.iti.domain.usecase.bookmark.ToggleBookmarkUseCase,
    private val observeBookmarks: com.iti.domain.usecase.bookmark.ObserveBookmarksUseCase,
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
    private var controlChannel: Channel<RecitationControl>? = null

     
    private var lastCursor: RecitationCursor? = null
    private var reconnectAttempts = 0

     
    private var isFinishing = false
    private var smoothedMicLevel = 0f

     
    private var seekOnPageLoad: Int? = null

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
                    it.copy(
                        isTajweedGradingEnabled = settings.tajweedGradingEnabled,
                        canGradeTajweed = settings.engineCanGradeTajweed,
                    )
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
        }
    }

    private fun updateWordHighlight(wordId: String?) {
        _state.update { state ->
            if (!state.areAyahsVisible && wordId != null) {
                state.copy(
                    highlightedWordId = wordId,
                    revealedWordIds = state.revealedWordIds + wordId,
                )
            } else {
                state.copy(highlightedWordId = wordId)
            }
        }
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
            it.copy(
                currentPage = clamped,
                
                
                
                highlightedWordId = if (wasLive) it.highlightedWordId else null,
                revealedWordIds = emptySet(),
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
        if (current.isRecordingActive) {
            
            
            clearLiveSession()
            _state.update { it.copy(isRecordingActive = false) }
        }

        _state.update {
            it.copy(
                mushafMode = mode,
                highlightedWordId = null,
                revealedWordIds = emptySet(),
                captureError = null,
            )
        }

        when (mode) {
            MushafMode.LISTEN -> {
                // Audio will start when the user explicitly taps play.
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
                startLiveRecitation(
                    config = recitationSettings.toConfig(lastCursor),
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

    private fun mergeChunk(chunk: RecitationChunk) {
        chunk.cursor?.let { lastCursor = it }
        Log.d(
            TAG,
            "Chunk ${chunk.sequence}: ${chunk.words.size} words, " +
                "${chunk.mistakeWords.size} mistakes, cursor=${chunk.cursor?.wordId}",
        )

        advancePageIfRecitationMovedOn(chunk.cursor)

        _state.update { state ->
            val live = state.liveCorrection
            state.copy(
                highlightedWordId = chunk.cursor?.wordId ?: state.highlightedWordId,
                liveCorrection = live.copy(
                    wordFeedback = live.wordFeedback.mergedWith(chunk),
                    candidates = (chunk.match as? RecitationMatch.Ambiguous)?.candidates.orEmpty(),
                    nonVerse = chunk.nonVerse,
                    lastOutcome = chunk.match.toOutcome(),
                    cursor = chunk.cursor ?: live.cursor,
                ),
            )
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


    /**
     * Sets the initial highlight for a page load/session start - a known deterministic anchor
     * (never a guess): [lastCursor] when it actually falls on the page now on screen
     * (recitation-driven page-follow already updates [lastCursor] to a confirmed cursor before
     * calling this, via [mergeChunk]/[advancePageIfRecitationMovedOn] - so it's already the right
     * anchor there), or the new page's own first word otherwise (a manual page turn away from
     * wherever the reciter last confirmed - continuing from a stale, off-screen cursor would
     * anchor the highlight to content that isn't even displayed anymore). Superseded by the first
     * server-confirmed chunk ([mergeChunk]) as soon as one arrives.
     */
    private fun seedInitialHighlightForCurrentPage() {
        val pageWords = _state.value.wordsForCurrentPage()
        val anchor = lastCursor?.takeIf { cursor -> pageWords.any { it.id == cursor.wordId } }
            ?: startCursorForCurrentPage()
            ?: return
        _state.update { it.copy(highlightedWordId = anchor.wordId) }
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

    







 
    private fun setTajweedGrading(enabled: Boolean) {
        val current = recitationSettings
        if (current.tajweedGradingEnabled == enabled) return

        val updated = current.copy(tajweedGradingEnabled = enabled)
        
        
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
            Log.d(TAG, "Tajwid grading -> $enabled; reopening the session at ${lastCursor?.wordId}")
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
        sessionStartedAtEpochMs = 0L
        pendingRestart = null
        _state.update {
            it.copy(
                micLevel = 0f,
                isSpeechDetected = false,
                highlightedWordId = null,
                liveCorrection = LiveCorrectionUiState(),
            )
        }
    }

     
    private fun seekLiveCorrection(cursor: RecitationCursor) {
        lastCursor = cursor
        controlChannel?.trySend(RecitationControl.Seek(cursor))
        Log.d(TAG, "Seek → ${cursor.wordId}")
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
                val timings = fetchTimingsForPage(page.pageNumber, reciter.id)
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

    private suspend fun fetchTimingsForPage(pageNumber: Int, reciterId: Int): List<AyahTiming> {
        return try {
            val result = getAyahTimings(reciterId, pageNumber).first()
            if (result is Result.Success) {
                Log.d(TAG, "Loaded ${result.data.size} timings for page $pageNumber")
                result.data
            } else {
                Log.e(TAG, "getAyahTimings returned non-success for page $pageNumber: $result")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ayah timings for page $pageNumber", e)
            emptyList()
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

    override fun onCleared() {
        clearLiveSession()
        simulatedHighlightDriver.stop()
        audioHighlightDriver.stop()
        playbackManager.release()
        super.onCleared()
    }
}
