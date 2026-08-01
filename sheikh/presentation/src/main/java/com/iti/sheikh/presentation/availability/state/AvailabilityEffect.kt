package com.iti.sheikh.presentation.availability.state

sealed interface AvailabilityEffect {
    data class ShowMessage(val message: String) : AvailabilityEffect
}
