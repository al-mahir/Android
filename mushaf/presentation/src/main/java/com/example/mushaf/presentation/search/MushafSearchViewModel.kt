package com.example.mushaf.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.usecase.GetLastReadUseCase
import com.example.mushaf.domain.usecase.search.SearchAyahUseCase
import com.example.mushaf.domain.usecase.search.SearchJuzUseCase
import com.example.mushaf.domain.usecase.search.SearchSurahUseCase
import com.example.mushaf.domain.usecase.GetTargetPageUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MushafSearchViewModel(
    private val searchSurahUseCase: SearchSurahUseCase,
    private val searchJuzUseCase: SearchJuzUseCase,
    private val searchAyahUseCase: SearchAyahUseCase,
    private val getLastReadUseCase: GetLastReadUseCase,
    private val getTargetPageUseCase: GetTargetPageUseCase,
    private val saveLastPageUseCase: SaveLastPageUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _shouldNavigateToMushaf = MutableStateFlow(false)

    private val _ayahs = MutableStateFlow<List<com.example.mushaf.domain.model.AyahSearchResult>>(emptyList())
    private val _isPaginatingAyahs = MutableStateFlow(false)
    private val _hasReachedEndAyahs = MutableStateFlow(false)
    private var currentAyahOffset = 0
    private val AYAH_PAGE_SIZE = 50

    private val searchResultsFlow = _query.debounce(500L).flatMapLatest { query ->
        flow {
            if (query.isBlank()) {
                val allJuzs = searchJuzUseCase("").getOrDefault(emptyList())
                emit(MushafSearchStateUpdate(juzs = allJuzs))
            } else {
                val matchedSurahs = searchSurahUseCase(query).getOrDefault(emptyList())
                
                // Reset pagination
                currentAyahOffset = 0
                _hasReachedEndAyahs.value = false
                _ayahs.value = emptyList()
                _isPaginatingAyahs.value = true
                val res = searchAyahUseCase(query, AYAH_PAGE_SIZE, currentAyahOffset).getOrDefault(emptyList())
                if (res.size < AYAH_PAGE_SIZE) _hasReachedEndAyahs.value = true
                _ayahs.value = res
                _isPaginatingAyahs.value = false
                
                emit(MushafSearchStateUpdate(surahs = matchedSurahs))
            }
        }
    }

    val state: StateFlow<MushafSearchState> = combine(
        _query,
        searchResultsFlow,
        _ayahs,
        _isPaginatingAyahs,
        _hasReachedEndAyahs,
        getLastReadUseCase(),
        _shouldNavigateToMushaf
    ) { args ->
        val query = args[0] as String
        val results = args[1] as MushafSearchStateUpdate
        val ayahs = args[2] as List<com.example.mushaf.domain.model.AyahSearchResult>
        val isPaginating = args[3] as Boolean
        val hasReachedEnd = args[4] as Boolean
        val lastRead = args[5] as com.example.mushaf.domain.model.LastReadSession?
        val shouldNavigate = args[6] as Boolean
        
        MushafSearchState(
            query = query,
            surahs = results.surahs,
            juzs = results.juzs,
            ayahs = ayahs,
            isPaginatingAyahs = isPaginating,
            hasReachedEndAyahs = hasReachedEnd,
            lastReadSession = lastRead,
            shouldNavigateToMushaf = shouldNavigate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MushafSearchState()
    )

    fun onIntent(intent: MushafSearchIntent) {
        when (intent) {
            is MushafSearchIntent.UpdateQuery -> _query.update { intent.query }
            is MushafSearchIntent.SurahClicked -> {
                viewModelScope.launch {
                    getTargetPageUseCase.forSurah(intent.surah.number)?.let { navigateToPage(it) }
                }
            }
            is MushafSearchIntent.JuzClicked -> {
                viewModelScope.launch {
                    getTargetPageUseCase.forJuz(intent.juz.number)?.let { navigateToPage(it) }
                }
            }
            is MushafSearchIntent.HizbClicked -> { /* Not supported yet */ }
            is MushafSearchIntent.PageClicked -> {
                navigateToPage(intent.page)
            }
            is MushafSearchIntent.AyahClicked -> {
                viewModelScope.launch {
                    getTargetPageUseCase.forAyah(intent.ayah.surahNumber, intent.ayah.ayahNumber)?.let { navigateToPage(it) }
                }
            }
            is MushafSearchIntent.LoadNextAyahsPage -> loadNextAyahsPage()
            MushafSearchIntent.LastReadClicked -> {
                state.value.lastReadSession?.let { navigateToPage(it.page) }
            }
            is MushafSearchIntent.NavigateBottomTab -> { /* Navigate */ }
            MushafSearchIntent.ClearNavigationEffect -> {
                _shouldNavigateToMushaf.value = false
            }
        }
    }

    private fun navigateToPage(page: Int) {
        viewModelScope.launch {
            saveLastPageUseCase(page)
            _shouldNavigateToMushaf.value = true
        }
    }

    private fun loadNextAyahsPage() {
        if (_hasReachedEndAyahs.value || _isPaginatingAyahs.value) return
        _isPaginatingAyahs.value = true
        currentAyahOffset += AYAH_PAGE_SIZE
        
        viewModelScope.launch {
            val res = searchAyahUseCase(_query.value, AYAH_PAGE_SIZE, currentAyahOffset).getOrDefault(emptyList())
            if (res.size < AYAH_PAGE_SIZE) {
                _hasReachedEndAyahs.value = true
            }
            _ayahs.value = _ayahs.value + res
            _isPaginatingAyahs.value = false
        }
    }

    private data class MushafSearchStateUpdate(
        val surahs: List<com.example.mushaf.domain.model.Surah> = emptyList(),
        val juzs: List<com.example.mushaf.domain.model.Juz> = emptyList(),
        val hizbs: List<com.example.mushaf.domain.model.Hizb> = emptyList(),
        val pages: List<Int> = emptyList(),
        val ayahs: List<com.example.mushaf.domain.model.AyahSearchResult> = emptyList()
    )
}
