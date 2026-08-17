package com.iti.presentation.meetingrequest.request

import com.iti.domain.model.MeetingEligibility

sealed interface RequestUiState {
    data object Idle : RequestUiState

    /** Verifying the student's remaining minutes before the request is allowed to go out. */
    data object CheckingQuota : RequestUiState

    /**
     * The student cannot book right now — no subscription, expired package, or not enough
     * minutes left. Carries the reason so the screen can offer the right next step.
     */
    data class QuotaBlocked(val reason: MeetingEligibility) : RequestUiState

    data object Sending : RequestUiState
    data class Pending(val requestId: String, val expiresAt: String) : RequestUiState
    data class Accepted(
        val requestId: String,
        val token: String,
        val channelName: String,
        val userAccount: String,
    ) : RequestUiState
    data class Declined(val reason: String?) : RequestUiState
    data object Expired : RequestUiState
    data object Ended : RequestUiState
    data class AlreadyPending(val message: String) : RequestUiState
}
