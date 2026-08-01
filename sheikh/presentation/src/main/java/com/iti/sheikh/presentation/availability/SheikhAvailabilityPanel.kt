package com.iti.sheikh.presentation.availability

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.sheikh.presentation.availability.state.AvailabilityEffect
import com.iti.sheikh.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel

/**
 * The sheikh-side availability toggle + incoming-request card, embedded as chrome inside the
 * host app's own Sheikh Home screen.
 */
@Composable
fun SheikhAvailabilityPanel(
    onMeetingAccepted: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AvailabilityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is AvailabilityEffect.ShowMessage -> Unit
        }
    }

    // Drives navigation off state (not just the one-shot NavigateToCall effect above) so an
    // accepted call is always reachable even if the effect's collector missed it — e.g. a
    // recomposition gap right as Accept is tapped. StateFlow replays its latest value to any
    // (re)started collector, unlike the effect Channel, so this is the reliable path.
    val busyRequestId = (state as? AvailabilityUiState.Busy)?.requestId
    LaunchedEffect(busyRequestId) {
        val busy = state as? AvailabilityUiState.Busy ?: return@LaunchedEffect
        android.util.Log.d("MeetingLifecycle", "SheikhAvailabilityPanel: auto-navigating to Call requestId=${busy.requestId}")
        onMeetingAccepted(busy.requestId, busy.token, busy.channelName, busy.userAccount)
    }

    SheikhAvailabilityContent(
        state = state,
        onIntent = viewModel::onIntent,
        onRejoinCall = { busy ->
            onMeetingAccepted(busy.requestId, busy.token, busy.channelName, busy.userAccount)
        },
        modifier = modifier,
    )
}
