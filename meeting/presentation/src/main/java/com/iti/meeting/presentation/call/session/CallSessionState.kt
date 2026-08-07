package com.iti.meeting.presentation.call.session

import com.iti.meeting.presentation.call.state.CallUiState

data class CallSessionState(
    val requestId: String? = null,
    val channelName: String? = null,
    val userAccount: String? = null,
    val remoteDisplayName: String? = null,
    val callState: CallUiState = CallUiState.Idle,
) {
    val isLive: Boolean get() = callState !is CallUiState.Idle && callState !is CallUiState.Ended
}
