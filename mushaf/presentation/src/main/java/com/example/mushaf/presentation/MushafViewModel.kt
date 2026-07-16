package com.example.mushaf.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.presentation.highlight.SimulatedHighlightDriver
import com.example.mushaf.presentation.state.MushafIntent
import com.example.mushaf.presentation.state.MushafUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private val highlightDriver = SimulatedHighlightDriver(viewModelScope)

    private var initialized = false

    private val loadJobs = mutableMapOf<Int, Job>()

    init {
        highlightDriver.currentWordId
            .onEach { wordId -> _state.update { it.copy(highlightedWordId = wordId) } }
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

    private companion object {
        const val TAG = "Mushaf"
    }

    fun onIntent(intent: MushafIntent) {
        when (intent) {
            is MushafIntent.LoadPage -> loadPage(intent.page)
            is MushafIntent.ToggleTajweed -> toggleTajweed(intent.enabled)
            is MushafIntent.HighlightWord ->
                _state.update { it.copy(highlightedWordId = intent.wordId) }
            MushafIntent.StartFollowAlongPreview -> startFollowAlong()
            MushafIntent.StopFollowAlongPreview -> stopFollowAlong()
            MushafIntent.Retry -> requestPage(_state.value.currentPage)
        }
    }


    private fun loadPage(page: Int) {
        val clamped = MushafConstants.clampPage(page)
        Log.d(TAG, "loadPage(requested=$page, clamped=$clamped)")
        _state.update { it.copy(currentPage = clamped, highlightedWordId = null) }

        requestPage(clamped)
        requestPage(clamped - 1)
        requestPage(clamped + 1)

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
        // Font-directory swap only — no page reload, no layout shift.
        Log.d(TAG, "toggleTajweed(enabled=$enabled)")
        _state.update { it.copy(isTajweedEnabled = enabled) }
        viewModelScope.launch {
            runCatching { setTajweedEnabled(enabled) }
                .onFailure { Log.e(TAG, "Failed to persist tajweed=$enabled", it) }
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
