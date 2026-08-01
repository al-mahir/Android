package com.iti.presentation.sheikh.state

sealed interface SheikhDetailsIntent {
    data object Retry : SheikhDetailsIntent
    data class JoinCircle(val circleId: String) : SheikhDetailsIntent
}

sealed interface SheikhDetailsEffect {
    data object NavigateBack : SheikhDetailsEffect
    data class NavigateToJoiningCircle(val circleId: String) : SheikhDetailsEffect
}
