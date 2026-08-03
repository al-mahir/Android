package com.iti.presentation.bookmark

import com.iti.domain.model.BookmarkType

data class BookmarkState(
    val selectedTab: BookmarkTab = BookmarkTab.SURAH,
    val items: List<BookmarkListItem> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val pendingRemoval: BookmarkListItem? = null,
) {
    val visibleItems: List<BookmarkListItem>
        get() = items.filter { it.matchesTab(selectedTab) && it.matchesQuery(searchQuery) }
}

enum class BookmarkTab(val labelRes: Int, val type: BookmarkType) {
    SURAH(com.iti.presentation.R.string.bookmark_tab_surah, BookmarkType.SURAH),
    AYAH(com.iti.presentation.R.string.bookmark_tab_ayah, BookmarkType.AYAH),
    PAGE(com.iti.presentation.R.string.bookmark_tab_page, BookmarkType.PAGE),
    SHEIKH(com.iti.presentation.R.string.bookmark_tab_sheikh, BookmarkType.SHEIKH),
}


sealed interface BookmarkListItem {
    val bookmarkId: String

    data class SurahItem(
        override val bookmarkId: String,
        val surahNumber: Int,
        val nameAr: String,
        val nameEn: String,
        val ayahCount: Int,
        val startPage: Int?,
    ) : BookmarkListItem

    data class AyahItem(
        override val bookmarkId: String,
        val surahNumber: Int,
        val ayahNumber: Int,
        val ayahText: String?,
        val surahNameAr: String,
        val surahNameEn: String,
        val page: Int?,
    ) : BookmarkListItem

    data class PageItem(
        override val bookmarkId: String,
        val pageNumber: Int,
        val surahNameAr: String,
        val surahNameEn: String,
        val juz: Int,
    ) : BookmarkListItem

    data class SheikhItem(
        override val bookmarkId: String,
        val sheikhId: String,
        val name: String,
        val initials: String,
        val rating: Double,
    ) : BookmarkListItem
}

private fun BookmarkListItem.matchesTab(tab: BookmarkTab): Boolean = when (tab) {
    BookmarkTab.SURAH -> this is BookmarkListItem.SurahItem
    BookmarkTab.AYAH -> this is BookmarkListItem.AyahItem
    BookmarkTab.PAGE -> this is BookmarkListItem.PageItem
    BookmarkTab.SHEIKH -> this is BookmarkListItem.SheikhItem
}

private fun BookmarkListItem.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return when (this) {
        is BookmarkListItem.SurahItem -> nameAr.contains(query, ignoreCase = true) ||
            nameEn.contains(query, ignoreCase = true)
        is BookmarkListItem.AyahItem -> ayahText?.contains(query, ignoreCase = true) == true ||
            surahNameAr.contains(query, ignoreCase = true) ||
            surahNameEn.contains(query, ignoreCase = true)
        is BookmarkListItem.PageItem -> surahNameAr.contains(query, ignoreCase = true) ||
            surahNameEn.contains(query, ignoreCase = true) ||
            pageNumber.toString().contains(query)
        is BookmarkListItem.SheikhItem -> name.contains(query, ignoreCase = true)
    }
}

sealed interface BookmarkIntent {
    data class SelectTab(val tab: BookmarkTab) : BookmarkIntent
    data class SearchQueryChanged(val query: String) : BookmarkIntent
    data class RequestRemove(val item: BookmarkListItem) : BookmarkIntent
    data object ConfirmRemove : BookmarkIntent
    data object CancelRemove : BookmarkIntent
    data class OpenBookmark(val item: BookmarkListItem) : BookmarkIntent
}

sealed interface BookmarkEffect {
    data class OpenMushafAtPage(val page: Int) : BookmarkEffect
    data class OpenSheikhDetails(val sheikhId: String) : BookmarkEffect
}
