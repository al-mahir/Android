package com.example.mushaf.presentation.state

import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.ReadingMode

data class MushafUiState(
    val currentPage: Int = MushafConstants.FIRST_PAGE,
    val pages: Map<Int, MushafPage> = emptyMap(),
    val failedPages: Set<Int> = emptySet(),
    val isTajweedEnabled: Boolean = true,
    val highlightedWordId: String? = null,
    val pageCount: Int = MushafConstants.LAST_PAGE,
    val isFollowAlongActive: Boolean = false,
) {
    val readingMode: ReadingMode get() = ReadingMode.from(isTajweedEnabled)

    val page: MushafPage? get() = pages[currentPage]

    val isLoading: Boolean get() = currentPage !in pages && currentPage !in failedPages

    fun pageState(pageNumber: Int): PageLoadState = when {
        pages.containsKey(pageNumber) -> PageLoadState.Loaded(pages.getValue(pageNumber))
        pageNumber in failedPages -> PageLoadState.Failed
        else -> PageLoadState.Loading
    }
}

sealed interface PageLoadState {
    data class Loaded(val page: MushafPage) : PageLoadState
    data object Loading : PageLoadState
    data object Failed : PageLoadState
}
