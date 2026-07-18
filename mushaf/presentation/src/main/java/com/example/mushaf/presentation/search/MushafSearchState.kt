package com.example.mushaf.presentation.search

import com.example.mushaf.domain.model.LastReadSession
import com.example.mushaf.domain.model.MushafFilter
import com.example.mushaf.domain.model.Surah

data class MushafSearchState(
    val query: String = "",
    val selectedFilter: MushafFilter = MushafFilter.SURAH,
    val surahs: List<Surah> = emptyList(),
    val isLoading: Boolean = false,
    val lastReadSession: LastReadSession? = null
)
