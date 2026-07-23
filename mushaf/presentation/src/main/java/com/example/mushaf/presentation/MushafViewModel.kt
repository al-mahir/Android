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
import com.example.mushaf.domain.model.AyahTiming
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
import kotlinx.coroutines.Job
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

    private companion object {
        const val TAG = "Mushaf"
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
                if (!initialized || _state.value.currentPage != prefs.lastPage) {
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
            
            is MushafIntent.SelectReciter -> selectReciter(intent.reciter)
            MushafIntent.PlayPauseAudio -> playPauseAudio()
            is MushafIntent.SetAudioSpeed -> playbackManager.setSpeed(intent.speed)
            is MushafIntent.SeekAudio -> playbackManager.seekTo(intent.positionMs)
            MushafIntent.NextAyahAudio -> Unit // TODO: implement next ayah
            MushafIntent.PrevAyahAudio -> Unit // TODO: implement prev ayah

            // Surah Picker
            MushafIntent.ShowSurahPicker -> _state.update { it.copy(showSurahPicker = true) }
            MushafIntent.HideSurahPicker -> _state.update { it.copy(showSurahPicker = false) }
            is MushafIntent.NavigateToSurah -> navigateToSurah(intent.surahNumber)

            // Tajweed Legend
            MushafIntent.ShowTajweedLegend -> _state.update { it.copy(showTajweedLegend = true) }
            MushafIntent.HideTajweedLegend -> _state.update { it.copy(showTajweedLegend = false) }
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
        val idx = surahNumber - 1
        val startPage = com.example.mushaf.domain.model.MushafConstants.SURAH_START_PAGES
            .getOrElse(idx) { com.example.mushaf.domain.model.MushafConstants.FIRST_PAGE }
        _state.update { it.copy(showSurahPicker = false) }
        loadPage(startPage)
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
        if (current.isRecordingActive) _state.update { it.copy(isRecordingActive = false) }

        _state.update {
            it.copy(
                mushafMode = mode,
                highlightedWordId = null,
                revealedWordIds = emptySet(),
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
        _state.update { it.copy(isRecordingActive = nowRecording) }

        if (nowRecording) {
            state.page?.let { highlightDriver.start(it) }
        } else {
            highlightDriver.stop()
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
        simulatedHighlightDriver.stop()
        audioHighlightDriver.stop()
        playbackManager.release()
        super.onCleared()
    }
}
