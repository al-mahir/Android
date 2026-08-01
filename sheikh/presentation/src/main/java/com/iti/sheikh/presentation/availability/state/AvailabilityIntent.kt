package com.iti.sheikh.presentation.availability

sealed interface AvailabilityIntent {
    data class ToggleAvailability(val isAvailable: Boolean) : AvailabilityIntent
    data object Accept : AvailabilityIntent
    data object Decline : AvailabilityIntent
}



