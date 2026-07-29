package com.iti.sheikh.presentation.availability

sealed interface AvailabilityUiState {
    data object Offline : AvailabilityUiState
    data object Available : AvailabilityUiState
    data class IncomingRequest(
        val requestId: String,
        val studentName: String,
        val note: String,
        val expiresAt: String,
    ) : AvailabilityUiState
    data object Busy : AvailabilityUiState
}

