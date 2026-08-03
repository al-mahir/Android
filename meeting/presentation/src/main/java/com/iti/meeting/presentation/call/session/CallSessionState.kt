package com.iti.meeting.presentation.call.session

import com.iti.meeting.presentation.call.CallUiState

/**
 * The full session `CallSessionController` tracks — [CallUiState] alone (the old `CallViewModel`
 * state) has no identity, which is fine for a screen scoped to one call but not for a singleton
 * that must answer "is there a live call right now, and which one" for the foreground service,
 * the notification, and app-relaunch routing.
 */
data class CallSessionState(
    val requestId: String? = null,
    val channelName: String? = null,
    val userAccount: String? = null,
    val remoteDisplayName: String? = null,
    val callState: CallUiState = CallUiState.Idle,
) {
    val isLive: Boolean get() = callState !is CallUiState.Idle && callState !is CallUiState.Ended
}
