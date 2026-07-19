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
import com.example.mushaf.presentation.highlight.SimulatedHighlightDriver
import com.example.mushaf.presentation.state.MushafEffect
import com.example.mushaf.presentation.state.MushafIntent
import com.example.mushaf.presentation.state.MushafUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    private val _state = MutableStateFlow(MushafUiState())
    val state: StateFlow<MushafUiState> = _state.asStateFlow()

    private val _effects = Channel<MushafEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val highlightDriver = SimulatedHighlightDriver(viewModelScope)
    private var initialized = false
    private val loadJobs = mutableMapOf<Int, Job>()

    private companion object {
        const val TAG = "Mushaf"
    }

    init {
        highlightDriver.currentWordId
            .onEach { wordId ->
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

    private fun startFollowAlong() {
        val page = _state.value.page ?: return
        _state.update { it.copy(isFollowAlongActive = true) }
        highlightDriver.start(page)
    }

    private fun stopFollowAlong() {
        highlightDriver.stop()
        _state.update { it.copy(isFollowAlongActive = false, highlightedWordId = null) }
    }

    override fun onCleared() {
        highlightDriver.stop()
        super.onCleared()
    }
}
