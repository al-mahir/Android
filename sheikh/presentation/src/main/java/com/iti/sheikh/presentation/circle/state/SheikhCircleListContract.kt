package com.iti.sheikh.presentation.circle.state

import com.iti.meeting.domain.model.circle.Circle

data class SheikhCircleListUiState(
    val circles: List<Circle> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)

sealed interface SheikhCircleListIntent {
    data class CircleClicked(val circleId: String) : SheikhCircleListIntent
    data object CreateCircleClicked : SheikhCircleListIntent
    data object Retry : SheikhCircleListIntent
}

sealed interface SheikhCircleListEffect {
    data class OpenCircle(val circleId: String) : SheikhCircleListEffect
    data object OpenCreateCircle : SheikhCircleListEffect
}
