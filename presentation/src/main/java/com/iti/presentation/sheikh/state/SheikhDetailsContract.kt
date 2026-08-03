package com.iti.presentation.sheikh.state

sealed interface SheikhDetailsIntent {
    data object Retry : SheikhDetailsIntent
    data class CircleClicked(val circleId: String) : SheikhDetailsIntent
}

sealed interface SheikhDetailsEffect {
    data object NavigateBack : SheikhDetailsEffect
    data class OpenCircle(val circleId: String) : SheikhDetailsEffect
}
