package com.iti.presentation.meetingrequest.request

sealed interface RequestIntent {
    data class Send(val sheikhId: String, val sheikhName: String?, val note: String?) : RequestIntent
    data object Cancel : RequestIntent

    /** Cancels the *other* pending request surfaced by [com.iti.presentation.meetingrequest.request.RequestUiState.AlreadyPending]. */
    data object CancelExisting : RequestIntent

    /** Self-heal for a stale terminal state (e.g. a reused ViewModel still parked on `Ended`
     * from a previous flow) — resets to [com.iti.presentation.meetingrequest.request.RequestUiState.Idle]. */
    data object Reset : RequestIntent

    /** Rehydrates an already-in-flight request (e.g. the student tapped "View" on the Home
     * pending-request banner) without re-sending it. */
    data class Resume(val requestId: String, val expiresAt: String) : RequestIntent

    /** From [com.iti.presentation.meetingrequest.request.RequestUiState.QuotaBlocked] — go buy or renew a package. */
    data object BuyPackage : RequestIntent
}


