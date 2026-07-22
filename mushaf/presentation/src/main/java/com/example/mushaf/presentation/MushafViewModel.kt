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
import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationChunk
import com.example.mushaf.domain.model.recite.RecitationControl
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMatch
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

class MushafViewModel(
    private val getPage: GetPageUseCase,
    private val observeReaderPreferences: ObserveReaderPreferencesUseCase,
    private val setTajweedEnabled: SetTajweedEnabledUseCase,
    private val saveLastPage: SaveLastPageUseCase,
    private val getReciters: GetRecitersUseCase,
    private val getAyahTimings: GetAyahTimingsUseCase,
    private val playbackManager: AudioPlayer,
    private val startLiveRecitation: StartLiveRecitationUseCase,
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

    /** Resume point for a reconnect: the latest cursor any chunk reported. */
    private var lastCursor: RecitationCursor? = null
    private var reconnectAttempts = 0

    /** Set once the reciter asks to stop, so a closing socket is not mistaken for a drop. */
    private var isFinishing = false
    private var smoothedMicLevel = 0f

    /** Page whose seek is still owed because it had not finished loading when it was turned to. */
    private var seekOnPageLoad: Int? = null

    private companion object {
        const val TAG = "Mushaf"


        const val MIC_LEVEL_SMOOTHING = 0.3f

        const val MIC_LEVEL_GAIN = 4f

        const val CAPTURE_LOG_INTERVAL_MS = 1_000L

        const val CLIPPING_THRESHOLD = 0.99f

        /**
         * Reconnect attempts after a dropped session. Bounded on purpose: retrying forever
         * against an unreachable service looks exactly like a reciter making no mistakes.
         */
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
                // Claim the restore slot before loading, so the preferences observer treats
                // the reader as already initialised whichever of the two arrives first.
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
            is MushafIntent.CaptureFailed -> failCapture(intent.error)
            MushafIntent.DismissCaptureError -> _state.update { it.copy(captureError = null) }
            is MushafIntent.SelectMistake -> _state.update {
                it.copy(liveCorrection = it.liveCorrection.copy(selectedMistakeWordId = intent.wordId))
            }
            MushafIntent.DismissEngineNotice -> _state.update {
                it.copy(liveCorrection = it.liveCorrection.copy(engineSubstituted = false))
            }
            
            is MushafIntent.SelectReciter -> selectReciter(intent.reciter)
            MushafIntent.PlayPauseAudio -> playPauseAudio()
            is MushafIntent.SetAudioSpeed -> playbackManager.setSpeed(intent.speed)
            is MushafIntent.SeekAudio -> playbackManager.seekTo(intent.positionMs)
            MushafIntent.NextAyahAudio -> Unit // TODO: implement next ayah
            MushafIntent.PrevAyahAudio -> Unit // TODO: implement prev ayah
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
        _state.update { it.copy(currentReciter = reciter) }
        if (_state.value.mushafMode == MushafMode.LISTEN && _state.value.isFollowAlongActive) {
            // Reload audio for current page
            startFollowAlong()
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
        _state.update {
            it.copy(
                currentPage = clamped,
                highlightedWordId = null,
                revealedWordIds = emptySet(),
            )
        }
        requestPage(clamped)
        requestPage(clamped - 1)
        requestPage(clamped + 1)
        requestPage(clamped - 2)
        requestPage(clamped + 2)

        // A page turn during a live session moves the reciter. Without telling the service, the
        // tracker keeps searching around the old position and starts reporting mismatches that
        // are not mistakes. The page may not be cached yet, hence the retry once it loads.
        if (_state.value.liveCorrection.isActive) {
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
            // Abandon rather than flush: the reciter left the mode, so a graded tail of a
            // recitation they are no longer doing would be noise.
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
            MushafMode.LISTEN -> startFollowAlong()
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
            // RECITATION runs the live AI session; MUALLEM still runs the simulated highlight
            // until it is migrated onto the same pipeline.
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

    // ===== Live AI correction =====

    /**
     * Opens a live session for the current page and collects its events until the reciter stops.
     *
     * The session is seeded with the reciter's position whenever the page is loaded. Starting
     * without one puts the service into whole-muṣḥaf search, where the basmalah — the most
     * likely opening — comes back ambiguous.
     */
    private fun startLiveCorrection() {
        sessionJob?.cancel()
        lastCursor = startCursorForCurrentPage()
        isFinishing = false
        reconnectAttempts = 0

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

    /**
     * Runs the session, resuming from the last cursor if the connection drops.
     *
     * There is no session resumption server-side: a reconnect opens a new session seeded with
     * the last cursor seen, which costs only the in-flight chunk. Attempts are bounded, because
     * retrying forever against an unreachable service looks identical to a reciter making no
     * mistakes.
     */
    private suspend fun runSession(controls: Channel<RecitationControl>) {
        while (currentCoroutineContext().isActive) {
            try {
                startLiveRecitation(
                    config = LiveRecitationConfig(start = lastCursor, engine = "zipformer"),
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
                _state.update {
                    it.copy(
                        isRecordingActive = false,
                        micLevel = 0f,
                        isSpeechDetected = false,
                        liveCorrection = it.liveCorrection.copy(isConnecting = false, isActive = false),
                    )
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

        _state.update { state ->
            val live = state.liveCorrection
            state.copy(
                liveCorrection = live.copy(
                    // Merged, not replaced: a word on a chunk boundary is reported twice, and an
                    // unscored second report must not erase the verdict the first one earned.
                    wordFeedback = live.wordFeedback.mergedWith(chunk),
                    candidates = (chunk.match as? RecitationMatch.Ambiguous)?.candidates.orEmpty(),
                    nonVerse = chunk.nonVerse,
                    lastOutcome = chunk.match.toOutcome(),
                    cursor = chunk.cursor ?: live.cursor,
                ),
            )
        }
    }

    private fun RecitationMatch.toOutcome(): ChunkOutcome = when (this) {
        is RecitationMatch.Matched -> ChunkOutcome.GRADED
        is RecitationMatch.Ambiguous -> ChunkOutcome.AMBIGUOUS
        RecitationMatch.NoMatch -> ChunkOutcome.NO_MATCH
    }

    /**
     * Asks the service to flush and waits for it to acknowledge.
     *
     * Deliberately not a cancellation: `Finish` lets the server grade the utterance still in
     * flight, which is the last few seconds of what was just recited. Cancelling drops it.
     */
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

    /** Tears the session down without waiting for the server — mode changes, backgrounding. */
    private fun clearLiveSession() {
        isFinishing = true
        sessionJob?.cancel()
        sessionJob = null
        controlChannel?.close()
        controlChannel = null
        smoothedMicLevel = 0f
        _state.update {
            it.copy(micLevel = 0f, isSpeechDetected = false, liveCorrection = LiveCorrectionUiState())
        }
    }

    /** Tells the service the reciter moved, so tracking does not drift into false mismatches. */
    private fun seekLiveCorrection(cursor: RecitationCursor) {
        lastCursor = cursor
        controlChannel?.trySend(RecitationControl.Seek(cursor))
        Log.d(TAG, "Seek → ${cursor.wordId}")
    }

    /** The first āyah word on the page being read, or null before the page has loaded. */
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
        
        // Stop any currently playing audio before starting a new page
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
            val paddedS = timing.surahNumber.toString().padStart(3, '0')
            val paddedA = timing.ayahNumber.toString().padStart(3, '0')
            "${reciter.audioBaseUrl}${paddedS}${paddedA}.mp3"
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
