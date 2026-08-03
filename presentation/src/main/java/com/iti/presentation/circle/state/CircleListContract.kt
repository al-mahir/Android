package com.iti.presentation.circle.state

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleType

data class CircleListUiState(
    val circles: List<Circle> = emptyList(),
    val filteredCircles: List<Circle> = emptyList(),
    val searchQuery: String = "",
    val selectedType: CircleType? = null,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)

sealed interface CircleListIntent {
    data class SearchQueryChanged(val query: String) : CircleListIntent
    data class TypeSelected(val type: CircleType?) : CircleListIntent
    data class CircleClicked(val circleId: String) : CircleListIntent
    data object CreateCircleClicked : CircleListIntent
    data object Retry : CircleListIntent
}

sealed interface CircleListEffect {
    data class OpenCircle(val circleId: String) : CircleListEffect
    data object OpenCreateCircle : CircleListEffect
    data object NavigateBack : CircleListEffect
}

