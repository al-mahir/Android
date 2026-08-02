package com.iti.presentation.sheikh.state

sealed interface SheikhListIntent {
    data class SearchQueryChanged(val query: String) : SheikhListIntent
    data class FilterSelected(val filter: SheikhFilter) : SheikhListIntent
    data class SheikhClicked(val sheikhId: String) : SheikhListIntent
    data class ToggleSheikhBookmark(val sheikhId: String) : SheikhListIntent
    data object Retry : SheikhListIntent
}
