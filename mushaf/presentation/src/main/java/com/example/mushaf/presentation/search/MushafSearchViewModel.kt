package com.example.mushaf.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.model.MushafFilter
import com.example.mushaf.domain.usecase.GetLastReadUseCase
import com.example.mushaf.domain.usecase.search.SearchAyahUseCase
import com.example.mushaf.domain.usecase.search.SearchHizbUseCase
import com.example.mushaf.domain.usecase.search.SearchJuzUseCase
import com.example.mushaf.domain.usecase.search.SearchPageUseCase
import com.example.mushaf.domain.usecase.search.SearchSurahUseCase
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
    private val searchPageUseCase: SearchPageUseCase,
    private val searchHizbUseCase: SearchHizbUseCase,
    private val searchAyahUseCase: SearchAyahUseCase,
    private val getLastReadUseCase: GetLastReadUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(MushafFilter.AYAH)

    private val searchResultsFlow = combine(
        _query.debounce(500L),
        _filter
    ) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        flow {
            val stateUpdate = when (filter) {
                MushafFilter.SURAH -> {
                    val res = searchSurahUseCase(query).getOrDefault(emptyList())
                    MushafSearchStateUpdate(surahs = res)
                }
                MushafFilter.PARA -> {
                    val res = searchJuzUseCase(query).getOrDefault(emptyList())
                    MushafSearchStateUpdate(juzs = res)
                }
                MushafFilter.PAGE -> {
                    val res = searchPageUseCase(query).getOrDefault(emptyList())
                    MushafSearchStateUpdate(pages = res)
                }
                MushafFilter.HIJB -> {
                    val res = searchHizbUseCase(query).getOrDefault(emptyList())
                    MushafSearchStateUpdate(hizbs = res)
                }
                MushafFilter.AYAH -> {
                    val res = searchAyahUseCase(query).getOrDefault(emptyList())
                    MushafSearchStateUpdate(ayahs = res)
                }
            }
            emit(stateUpdate)
        }
    }

    val state: StateFlow<MushafSearchState> = combine(
        _query,
        _filter,
        searchResultsFlow,
        getLastReadUseCase()
    ) { query, filter, results, lastRead ->
        MushafSearchState(
            query = query,
            selectedFilter = filter,
            surahs = results.surahs,
            juzs = results.juzs,
            hizbs = results.hizbs,
            pages = results.pages,
            ayahs = results.ayahs,
            lastReadSession = lastRead
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MushafSearchState()
    )

    fun onIntent(intent: MushafSearchIntent) {
        when (intent) {
            is MushafSearchIntent.UpdateQuery -> _query.update { intent.query }
            is MushafSearchIntent.SelectFilter -> _filter.update { intent.filter }
            is MushafSearchIntent.SurahClicked -> { /* Navigate */ }
            is MushafSearchIntent.JuzClicked -> { /* Navigate */ }
            is MushafSearchIntent.HizbClicked -> { /* Navigate */ }
            is MushafSearchIntent.PageClicked -> { /* Navigate */ }
            is MushafSearchIntent.AyahClicked -> { /* Navigate */ }
            MushafSearchIntent.LastReadClicked -> { /* Navigate */ }
            is MushafSearchIntent.NavigateBottomTab -> { /* Navigate */ }
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
