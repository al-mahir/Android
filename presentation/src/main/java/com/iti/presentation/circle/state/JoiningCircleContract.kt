package com.iti.presentation.circle.state

import com.iti.domain.model.StudyCircle

data class JoiningCircleUiState(
    val circle: StudyCircle? = null,
    val isLoading: Boolean = true,
)

sealed interface JoiningCircleIntent {
    data object CancelRequest : JoiningCircleIntent
}

sealed interface JoiningCircleEffect {
    data object NavigateBack : JoiningCircleEffect
    data class NavigateToSession(val circleId: String) : JoiningCircleEffect
}
