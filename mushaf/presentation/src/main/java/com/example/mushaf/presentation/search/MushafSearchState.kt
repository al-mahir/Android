package com.example.mushaf.presentation.search

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.LastReadSession
import com.example.mushaf.domain.model.MushafFilter
import com.example.mushaf.domain.model.Surah

data class MushafSearchState(
    val query: String = "",
    val selectedFilter: MushafFilter = MushafFilter.AYAH,
    val surahs: List<Surah> = emptyList(),
    val juzs: List<Juz> = emptyList(),
    val hizbs: List<Hizb> = emptyList(),
    val pages: List<Int> = emptyList(),
    val ayahs: List<AyahSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val lastReadSession: LastReadSession? = null
)
