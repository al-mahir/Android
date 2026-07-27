package com.example.mushaf.presentation.search

import com.example.designsystem.text.UiText
import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.LastReadSession
import com.example.mushaf.domain.model.Surah

data class MushafSearchState(
    val query: String = "",
    val searchType: SearchType = SearchType.TEXT,
    val useHyDe: Boolean = true,
    val hydeUsed: Boolean = false,
    val surahs: List<Surah> = emptyList(),
    val juzs: List<Juz> = emptyList(),
    val hizbs: List<Hizb> = emptyList(),
    val pages: List<Int> = emptyList(),
    val ayahs: List<AyahSearchResult> = emptyList(),
    val tafsirs: List<com.example.mushaf.domain.model.TafsirResult> = emptyList(),
    val isPaginatingAyahs: Boolean = false,
    val hasReachedEndAyahs: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val lastReadSession: LastReadSession? = null,
    val shouldNavigateToMushaf: Boolean = false
)
