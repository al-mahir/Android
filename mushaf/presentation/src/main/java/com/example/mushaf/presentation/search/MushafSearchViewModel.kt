package com.example.mushaf.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.model.MushafFilter
import com.example.mushaf.domain.model.Surah
import com.example.mushaf.domain.usecase.GetLastReadUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MushafSearchViewModel(
    private val getLastReadUseCase: GetLastReadUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(MushafFilter.SURAH)
    
    // We will hold a static list of all Surahs for now, 
    // ideally this comes from a repository layer.
    private val allSurahs = listOf(
        Surah(1, "Al-Fatihah", "الفاتحة", "Meccan", 7),
        Surah(2, "Al-Baqarah", "البقرة", "Medinan", 286),
        Surah(3, "Ali Imran", "آل عمران", "Medinan", 200),
        Surah(4, "An-Nisa", "النساء", "Medinan", 176),
        Surah(5, "Al-Maidah", "المائدة", "Medinan", 120),
        Surah(6, "Al-Anam", "الأنعام", "Meccan", 165)
    )

    val state: StateFlow<MushafSearchState> = combine(
        _query.debounce(500L),
        _filter,
        getLastReadUseCase()
    ) { query, filter, lastRead ->
        val filteredSurahs = if (filter == MushafFilter.SURAH) {
            if (query.isBlank()) {
                allSurahs
            } else {
                allSurahs.filter {
                    it.nameEn.contains(query, ignoreCase = true) ||
                    it.nameAr.contains(query, ignoreCase = true)
                }
            }
        } else {
            emptyList() // Other tabs will show empty for now
        }
        
        MushafSearchState(
            query = _query.value,
            selectedFilter = filter,
            surahs = filteredSurahs,
            lastReadSession = lastRead
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MushafSearchState()
    )

    fun onIntent(intent: MushafSearchIntent) {
        when (intent) {
            is MushafSearchIntent.UpdateQuery -> {
                _query.update { intent.query }
            }
            is MushafSearchIntent.SelectFilter -> {
                _filter.update { intent.filter }
            }
            is MushafSearchIntent.SurahClicked -> {
                // Navigate to Mushaf screen for this Surah
            }
            MushafSearchIntent.LastReadClicked -> {
                // Navigate to last read position
            }
            is MushafSearchIntent.NavigateBottomTab -> {
                // Handle bottom navigation
            }
        }
    }
}
