package com.iti.presentation.meetingrequest.request



sealed interface RequestUiState {
    data object Idle : RequestUiState
    data object Sending : RequestUiState
    data class Pending(val requestId: String, val expiresAt: String) : RequestUiState
    data class Accepted(
        val circleId: String,
        val token: String,
        val channelName: String,
        val uid: Int,
    ) : RequestUiState
    data class Declined(val reason: String?) : RequestUiState
    data object Expired : RequestUiState
}


