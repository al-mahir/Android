package com.iti.sheikh.presentation.availability

sealed interface AvailabilityEffect {
    data class NavigateToCall(
        val circleId: String,
        val token: String,
        val channelName: String,
        val uid: Int,
    ) : AvailabilityEffect
    data class ShowMessage(val message: String) : AvailabilityEffect
}



