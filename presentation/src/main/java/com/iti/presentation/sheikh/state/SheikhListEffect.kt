package com.iti.presentation.sheikh.state

sealed interface SheikhListEffect {
    data class NavigateToSheikhDetails(val sheikhId: String) : SheikhListEffect
    data object NavigateBack : SheikhListEffect
}
