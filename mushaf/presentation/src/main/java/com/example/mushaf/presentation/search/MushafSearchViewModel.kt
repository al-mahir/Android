package com.example.mushaf.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.usecase.GetLastReadUseCase
import com.example.mushaf.domain.usecase.search.SearchAyahUseCase
import com.example.mushaf.domain.usecase.search.SearchJuzUseCase
import com.example.mushaf.domain.usecase.search.SearchSurahUseCase
import com.example.mushaf.domain.usecase.GetTargetPageUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.presentation.core.error.toUiText
import com.example.designsystem.text.UiText
import com.iti.domain.core.fold
import com.iti.domain.core.getOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.mushaf.domain.usecase.search.SearchAyahByMeaningUseCase

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MushafSearchViewModel(
    private val searchSurahUseCase: SearchSurahUseCase,
    private val searchJuzUseCase: SearchJuzUseCase,
    private val searchAyahUseCase: SearchAyahUseCase,
    private val searchAyahByMeaningUseCase: SearchAyahByMeaningUseCase,
    private val searchTafsirUseCase: com.example.mushaf.domain.usecase.search.SearchTafsirUseCase,
    private val getLastReadUseCase: GetLastReadUseCase,
    private val getTargetPageUseCase: GetTargetPageUseCase,
    private val saveLastPageUseCase: SaveLastPageUseCase,
    private val connectivityObserver: com.iti.domain.connectivity.ConnectivityObserver,
    private val toggleBookmarkUseCase: com.iti.domain.usecase.bookmark.ToggleBookmarkUseCase,
    private val observeBookmarks: com.iti.domain.usecase.bookmark.ObserveBookmarksUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _searchType = MutableStateFlow(SearchType.TEXT)
    private val _useHyDe = MutableStateFlow(true)
    private val _shouldNavigateToMushaf = MutableStateFlow(false)

    private val _ayahs = MutableStateFlow<List<com.example.mushaf.domain.model.AyahSearchResult>>(emptyList())
    private val _tafsirs = MutableStateFlow<List<com.example.mushaf.domain.model.TafsirResult>>(emptyList())
    private val _isPaginatingAyahs = MutableStateFlow(false)
    private val _hasReachedEndAyahs = MutableStateFlow(false)
    private val _hydeUsed = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<UiText?>(null)
    private var currentAyahOffset = 0
    private val AYAH_PAGE_SIZE = 50

    private val bookmarkedSurahsFlow: kotlinx.coroutines.flow.Flow<Set<Int>> =
        observeBookmarks(com.iti.domain.model.BookmarkType.SURAH).map { result ->
            result.getOrNull()?.mapNotNull { it.surahNumber }?.toSet() ?: emptySet()
        }

    private val bookmarkedAyahsFlow: kotlinx.coroutines.flow.Flow<Set<Pair<Int, Int>>> =
        observeBookmarks(com.iti.domain.model.BookmarkType.AYAH).map { result ->
            result.getOrNull()?.mapNotNull { bookmark ->
                val surah = bookmark.surahNumber
                val ayah = bookmark.ayahNumber
                if (surah != null && ayah != null) surah to ayah else null
            }?.toSet() ?: emptySet()
        }

    private val searchResultsFlow = combine(
        _query.debounce(500L), 
        _searchType, 
        _useHyDe, 
        connectivityObserver.status
    ) { query, searchType, useHyDe, connectionStatus ->
        Quadruple(query, searchType, useHyDe, connectionStatus)
    }.flatMapLatest { (query, searchType, useHyDe, connectionStatus) ->
        flow {
            _errorMessage.value = null
            _hydeUsed.value = false
            if (query.isBlank()) {
                val allJuzs = searchJuzUseCase("").getOrNull() ?: emptyList()
                emit(MushafSearchStateUpdate(juzs = allJuzs))
            } else if (searchType == SearchType.TEXT) {
                val matchedSurahs = searchSurahUseCase(query).getOrNull() ?: emptyList()

                // Reset pagination
                currentAyahOffset = 0
                _hasReachedEndAyahs.value = false
                _ayahs.value = emptyList()
                _isPaginatingAyahs.value = true
                val res = searchAyahUseCase(query, AYAH_PAGE_SIZE, currentAyahOffset).getOrNull() ?: emptyList()
                if (res.size < AYAH_PAGE_SIZE) _hasReachedEndAyahs.value = true
                _ayahs.value = res
                _isPaginatingAyahs.value = false

                emit(MushafSearchStateUpdate(surahs = matchedSurahs))
            } else if (searchType == SearchType.TAFSIR) {
                // Search Tafsir Meaning
                currentAyahOffset = 0
                _hasReachedEndAyahs.value = false
                _ayahs.value = emptyList()
                _tafsirs.value = emptyList()
                _isPaginatingAyahs.value = true
                val res = searchTafsirUseCase(query, AYAH_PAGE_SIZE, currentAyahOffset).getOrNull() ?: emptyList()
                if (res.size < AYAH_PAGE_SIZE) _hasReachedEndAyahs.value = true
                _tafsirs.value = res
                _isPaginatingAyahs.value = false

                emit(MushafSearchStateUpdate(surahs = emptyList()))
            } else {
                // Search by Meaning (Semantic / Hybrid search via Backend AI service)
                currentAyahOffset = 0
                _hasReachedEndAyahs.value = true
                _ayahs.value = emptyList()
                _tafsirs.value = emptyList()
                _isPaginatingAyahs.value = true
                
                if (connectionStatus == com.iti.domain.connectivity.ConnectivityStatus.Unavailable) {
                    _errorMessage.value = UiText.Resource(com.example.mushaf.presentation.R.string.search_meaning_offline)
                    _isPaginatingAyahs.value = false
                    emit(MushafSearchStateUpdate(surahs = emptyList()))
                    return@flow
                }

                val result = searchAyahByMeaningUseCase(
                    query = query,
                    mode = "hybrid",
                    hyde = useHyDe,
                    limit = 20
                )

                result.fold(
                    onSuccess = { res ->
                        _ayahs.value = res
                        _hydeUsed.value = res.firstOrNull()?.hydeUsed ?: false
                    },
                    onError = { error ->
                        _errorMessage.value = error.toUiText()
                    },
                )

                _isPaginatingAyahs.value = false
                emit(MushafSearchStateUpdate(surahs = emptyList()))
            }
        }
    }

    val state: StateFlow<MushafSearchState> = combine(
        _query,
        _searchType,
        _useHyDe,
        searchResultsFlow,
        _ayahs,
        _tafsirs,
        _isPaginatingAyahs,
        _hasReachedEndAyahs,
        _hydeUsed,
        _errorMessage,
        getLastReadUseCase(),
        _shouldNavigateToMushaf,
        bookmarkedSurahsFlow,
        bookmarkedAyahsFlow,
    ) { args ->
        val query = args[0] as String
        val searchType = args[1] as SearchType
        val useHyDe = args[2] as Boolean
        val results = args[3] as MushafSearchStateUpdate
        val ayahs = args[4] as List<com.example.mushaf.domain.model.AyahSearchResult>
        val tafsirs = args[5] as List<com.example.mushaf.domain.model.TafsirResult>
        val isPaginating = args[6] as Boolean
        val hasReachedEnd = args[7] as Boolean
        val hydeUsed = args[8] as Boolean
        val errorMessage = args[9] as UiText?
        val lastRead = args[10] as com.example.mushaf.domain.model.LastReadSession?
        val shouldNavigate = args[11] as Boolean
        @Suppress("UNCHECKED_CAST")
        val bookmarkedSurahs = args[12] as Set<Int>
        @Suppress("UNCHECKED_CAST")
        val bookmarkedAyahs = args[13] as Set<Pair<Int, Int>>

        MushafSearchState(
            query = query,
            searchType = searchType,
            useHyDe = useHyDe,
            hydeUsed = hydeUsed,
            surahs = results.surahs,
            juzs = results.juzs,
            ayahs = ayahs,
            tafsirs = tafsirs,
            isPaginatingAyahs = isPaginating,
            hasReachedEndAyahs = hasReachedEnd,
            errorMessage = errorMessage,
            lastReadSession = lastRead,
            shouldNavigateToMushaf = shouldNavigate,
            bookmarkedSurahs = bookmarkedSurahs,
            bookmarkedAyahs = bookmarkedAyahs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MushafSearchState()
    )

    fun onIntent(intent: MushafSearchIntent) {
        when (intent) {
            is MushafSearchIntent.UpdateQuery -> _query.update { intent.query }
            is MushafSearchIntent.SelectSearchType -> _searchType.update { intent.searchType }
            is MushafSearchIntent.ToggleHyDe -> _useHyDe.update { intent.enabled }
            is MushafSearchIntent.SurahClicked -> {
                viewModelScope.launch {
                    getTargetPageUseCase.forSurah(intent.surah.number).getOrNull()?.let { navigateToPage(it) }
                }
            }
            is MushafSearchIntent.JuzClicked -> {
                viewModelScope.launch {
                    getTargetPageUseCase.forJuz(intent.juz.number).getOrNull()?.let { navigateToPage(it) }
                }
            }
            is MushafSearchIntent.HizbClicked -> { /* Not supported yet */ }
            is MushafSearchIntent.PageClicked -> {
                navigateToPage(intent.page)
            }
            is MushafSearchIntent.AyahClicked -> {
                viewModelScope.launch {
                    getTargetPageUseCase.forAyah(intent.ayah.surahNumber, intent.ayah.ayahNumber).getOrNull()
                        ?.let { navigateToPage(it) }
                }
            }
            is MushafSearchIntent.TafsirClicked -> {
                viewModelScope.launch {
                    getTargetPageUseCase.forAyah(intent.tafsir.surahNumber, intent.tafsir.ayahNumber).getOrNull()
                        ?.let { navigateToPage(it) }
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
            is MushafSearchIntent.ToggleSurahBookmark -> {
                viewModelScope.launch {
                    val page = getTargetPageUseCase.forSurah(intent.surah.number).getOrNull()
                    val bookmark = com.iti.domain.model.Bookmark(
                        id = "",
                        type = com.iti.domain.model.BookmarkType.SURAH,
                        surahNumber = intent.surah.number,
                        pageNumber = page,
                        createdAtEpochMillis = System.currentTimeMillis()
                    )
                    toggleBookmarkUseCase(bookmark)
                }
            }
            is MushafSearchIntent.ToggleAyahBookmark -> {
                viewModelScope.launch {
                    val page = getTargetPageUseCase.forAyah(intent.surahNumber, intent.ayahNumber).getOrNull()
                    val bookmark = com.iti.domain.model.Bookmark(
                        id = "",
                        type = com.iti.domain.model.BookmarkType.AYAH,
                        surahNumber = intent.surahNumber,
                        ayahNumber = intent.ayahNumber,
                        pageNumber = page,
                        createdAtEpochMillis = System.currentTimeMillis()
                    )
                    toggleBookmarkUseCase(bookmark)
                }
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
            if (_searchType.value == SearchType.TEXT) {
                val newAyahs = searchAyahUseCase(_query.value, AYAH_PAGE_SIZE, currentAyahOffset).getOrNull() ?: emptyList()
                if (newAyahs.size < AYAH_PAGE_SIZE) {
                    _hasReachedEndAyahs.value = true
                }
                _ayahs.value = _ayahs.value + newAyahs
            } else if (_searchType.value == SearchType.TAFSIR) {
                val newTafsirs = searchTafsirUseCase(_query.value, AYAH_PAGE_SIZE, currentAyahOffset).getOrNull() ?: emptyList()
                if (newTafsirs.size < AYAH_PAGE_SIZE) {
                    _hasReachedEndAyahs.value = true
                }
                _tafsirs.value = _tafsirs.value + newTafsirs
            }
            _isPaginatingAyahs.value = false
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private data class MushafSearchStateUpdate(
        val surahs: List<com.example.mushaf.domain.model.Surah> = emptyList(),
        val juzs: List<com.example.mushaf.domain.model.Juz> = emptyList(),
        val hizbs: List<com.example.mushaf.domain.model.Hizb> = emptyList(),
        val pages: List<Int> = emptyList(),
        val ayahs: List<com.example.mushaf.domain.model.AyahSearchResult> = emptyList()
    )
}
