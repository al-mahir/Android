package com.example.mushaf.presentation.search

import com.example.mushaf.domain.model.Surah

sealed interface MushafSearchIntent {
    data class UpdateQuery(val query: String) : MushafSearchIntent
    data class SurahClicked(val surah: Surah) : MushafSearchIntent
    data class JuzClicked(val juz: com.example.mushaf.domain.model.Juz) : MushafSearchIntent
    data class HizbClicked(val hizb: com.example.mushaf.domain.model.Hizb) : MushafSearchIntent
    data class PageClicked(val page: Int) : MushafSearchIntent
    data class AyahClicked(val ayah: com.example.mushaf.domain.model.AyahSearchResult) : MushafSearchIntent
    data class TafsirClicked(val tafsir: com.example.mushaf.domain.model.TafsirResult) : MushafSearchIntent
    data object LoadNextAyahsPage : MushafSearchIntent
    data object LastReadClicked : MushafSearchIntent
    data class SelectSearchType(val searchType: SearchType) : MushafSearchIntent
    data class ToggleHyDe(val enabled: Boolean) : MushafSearchIntent
    data class NavigateBottomTab(val tabIndex: Int) : MushafSearchIntent
    data object ClearNavigationEffect : MushafSearchIntent
}
