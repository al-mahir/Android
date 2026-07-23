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
import com.example.mushaf.domain.model.recite.RecitationPacer
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
import kotlinx.coroutines.launch
import java.util.UUID

class MushafViewModel(
    private val getPage: GetPageUseCase,
    private val observeReaderPreferences: ObserveReaderPreferencesUseCase,
    private val setTajweedEnabled: SetTajweedEnabledUseCase,
    private val saveLastPage: SaveLastPageUseCase,
    private val getReciters: GetRecitersUseCase,
    private val getAyahTimings: GetAyahTimingsUseCase,
    private val playbackManager: AudioPlayer,
    private val startLiveRecitation: StartLiveRecitationUseCase,
    private val saveRecitationSession: SaveRecitationSessionUseCase,
    private val observeRecitationSettings: ObserveRecitationSettingsUseCase,
    private val updateRecitationSettings: UpdateRecitationSettingsUseCase,
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

    


 
    private val pacer = RecitationPacer()

    private var chunkSpeechFrames = 0

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
                                kotlin.runCatching {
                                    getPage(nextPage).first()
                                }.getOrNull()?.let { loaded ->
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
                _state.update { it.copy(isTajweedEnabled = prefs.tajweedEnabled) }
                if (!initialized) {
                    initialized = true
                    onIntent(MushafIntent.LoadPage(prefs.lastPage))
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
            MushafIntent.NextAyahAudio -> Unit 
            MushafIntent.PrevAyahAudio -> Unit 
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
        _state.update { it.copy(currentReciter = reciter) }
        if (_state.value.mushafMode == MushafMode.LISTEN && _state.value.isFollowAlongActive) {
            if (wasPlaying) {
                startFollowAlong()
            } else {
                _state.update { it.copy(isFollowAlongActive = false) }
            }
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
            
            
            seedPacerForCurrentPage()
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
            .onEach { loaded ->
                Log.d(TAG, "Page $page loaded with ${loaded.lines.size} lines")
                _state.update {
                    it.copy(
                        pages = it.pages + (page to loaded),
                        failedPages = it.failedPages - page,
                    )
                }

                if (page == _state.value.currentPage && _state.value.isRecordingActive) {
                    seedPacerForCurrentPage()
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
        lastCursor = resumeAt ?: startCursorForCurrentPage()
        isFinishing = false
        reconnectAttempts = 0
        pacer.reset()
        chunkSpeechFrames = 0
        sessionStartedAtEpochMs = System.currentTimeMillis()
        seedPacerForCurrentPage()

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
                ).collect { event -> onLiveEvent(event) }
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

        
        
        val followedWordId = if (event.isSpeaking) {
            chunkSpeechFrames++
            pacer.onSpeechFrame()
        } else {
            pacer.currentWordId
        }

        _state.update {
            it.copy(
                micLevel = smoothedMicLevel.coerceIn(0f, 1f),
                isSpeechDetected = event.isSpeaking,
                highlightedWordId = followedWordId ?: it.highlightedWordId,
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

        
        
        
        if (chunk.words.isNotEmpty()) {
            pacer.observePace(wordCount = chunk.words.size, speechFrames = chunkSpeechFrames)
        }
        chunkSpeechFrames = 0
        chunk.cursor?.let { pacer.confirm(it.wordId) }
        advancePageIfRecitationMovedOn(chunk.cursor)

        _state.update { state ->
            val live = state.liveCorrection
            state.copy(
                highlightedWordId = pacer.currentWordId ?: state.highlightedWordId,
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


    private fun seedPacerForCurrentPage() {
        val words = _state.value.wordsForCurrentPage().filterNot { it.isEndOfAyah }.map { it.id }
        if (words.isEmpty()) return
        pacer.setWords(words)

        lastCursor?.wordId?.let { pacer.confirm(it) }
        if (pacer.currentWordId == null) pacer.placeAtStart()

        pacer.currentWordId?.let { wordId ->
            _state.update { it.copy(highlightedWordId = wordId) }
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
        pacer.reset()
        chunkSpeechFrames = 0
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

    private fun startFollowAlong(targetPageNumber: Int = _state.value.currentPage) {
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
                    val urls = buildAudioUrls(timings, reciter)
                    
                    if (urls.isNotEmpty() && timings.isNotEmpty()) {
                        val firstSurah = timings.first().surahNumber
                        Log.d(TAG, "First surah on page: $firstSurah")
                        Log.d(TAG, "requested link is : ${urls.first()}")
                    }
                    
                    Log.d(TAG, "Playing ${urls.size} audio URLs for this page")
                    playbackManager.playUrls(urls)
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

    private fun buildAudioUrls(timings: List<AyahTiming>, reciter: Reciter): List<String> {
        return timings.map { timing ->
            val audioUrl = timing.audioUrl
            if (audioUrl != null) {
                if (audioUrl.startsWith("http")) audioUrl
                else if (audioUrl.startsWith("//")) "https:$audioUrl"
                else "https://audio.qurancdn.com/${audioUrl.removePrefix("/")}"
            } else {
                val paddedS = timing.surahNumber.toString().padStart(3, '0')
                val paddedA = timing.ayahNumber.toString().padStart(3, '0')
                "${reciter.audioBaseUrl}${paddedS}${paddedA}.mp3"
            }
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
