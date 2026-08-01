package com.iti.presentation.meetingrequest.request

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.meetingrequest.request.RequestEffect
import com.iti.presentation.meetingrequest.request.RequestIntent
import org.koin.androidx.compose.koinViewModel

@Composable
fun MeetingRequestScreen(
    sheikhId: String,
    sheikhName: String? = null,
    onBack: () -> Unit,
    onMeetingAccepted: (String, String, String, String) -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: MeetingRequestViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is RequestEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    // Drives navigation off state (not a one-shot effect) so an accepted call is always
    // reachable even if a collector gap would've missed a Channel-based effect — see
    // AvailabilityViewModel.accept()'s KDoc on the sheikh side for the original diagnosis.
    val acceptedRequestId = (state as? RequestUiState.Accepted)?.requestId
    LaunchedEffect(acceptedRequestId) {
        val accepted = state as? RequestUiState.Accepted ?: return@LaunchedEffect
        onMeetingAccepted(accepted.requestId, accepted.token, accepted.channelName, accepted.userAccount)
    }

    // Self-heals a ViewModel instance stuck on a terminal state from a previous flow (e.g. a
    // reused Navigation3 entry still parked on `Ended`), and rehydrates an already-sent request
    // when arriving here from the Home "pending request" banner rather than a fresh Send.
    LaunchedEffect(sheikhId) {
        when (val initial = viewModel.state.value) {
            RequestUiState.Ended, is RequestUiState.Declined, RequestUiState.Expired ->
                viewModel.onIntent(RequestIntent.Reset)

            RequestUiState.Idle -> {
                val pending = viewModel.currentPendingRequestFor(sheikhId)
                if (pending != null) {
                    viewModel.onIntent(RequestIntent.Resume(pending.requestId, pending.expiresAt))
                }
            }

            else -> Unit
        }
    }

    MeetingRequestContent(
        state = state,
        onSend = { note -> viewModel.onIntent(RequestIntent.Send(sheikhId, sheikhName, note)) },
        onCancel = { viewModel.onIntent(RequestIntent.Cancel) },
        onCancelExisting = { viewModel.onIntent(RequestIntent.CancelExisting) },
        onBack = onBack,
    )
}


