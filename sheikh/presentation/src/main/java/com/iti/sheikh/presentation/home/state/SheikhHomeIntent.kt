package com.iti.sheikh.presentation.home.state

sealed interface SheikhHomeIntent {
    data object ProfileClicked : SheikhHomeIntent
    data class AvailabilityToggled(val isAvailable: Boolean) : SheikhHomeIntent
    data object Retry : SheikhHomeIntent
}
