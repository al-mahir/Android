package com.iti.sheikh.presentation.availability

sealed interface AvailabilityEffect {
    data class NavigateToCall(
        val requestId: String,
        val token: String,
        val channelName: String,
        val userAccount: String,
    ) : AvailabilityEffect
    data class ShowMessage(val message: String) : AvailabilityEffect
}
