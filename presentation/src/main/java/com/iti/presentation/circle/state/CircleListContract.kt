package com.iti.presentation.circle.state

import com.iti.domain.model.StudyCircle

data class CircleListUiState(
    val circles: List<StudyCircle> = emptyList(),
    val filteredCircles: List<StudyCircle> = emptyList(),
    val searchQuery: String = "",
    val selectedTag: String = TAG_ALL,
    val availableTags: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
) {
    companion object {
        const val TAG_ALL = "All"
    }
}

sealed interface CircleListIntent {
    data class SearchQueryChanged(val query: String) : CircleListIntent
    data class TagSelected(val tag: String) : CircleListIntent
    data class JoinCircle(val circleId: String) : CircleListIntent
    data object Retry : CircleListIntent
}

sealed interface CircleListEffect {
    data class NavigateToJoiningCircle(val circleId: String) : CircleListEffect
    data object NavigateBack : CircleListEffect
}
