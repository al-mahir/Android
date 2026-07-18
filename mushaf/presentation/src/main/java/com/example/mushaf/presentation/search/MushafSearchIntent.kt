package com.example.mushaf.presentation.search

import com.example.mushaf.domain.model.MushafFilter
import com.example.mushaf.domain.model.Surah

sealed interface MushafSearchIntent {
    data class UpdateQuery(val query: String) : MushafSearchIntent
    data class SelectFilter(val filter: MushafFilter) : MushafSearchIntent
    data class SurahClicked(val surah: Surah) : MushafSearchIntent
    data object LastReadClicked : MushafSearchIntent
    data class NavigateBottomTab(val tabIndex: Int) : MushafSearchIntent
}
