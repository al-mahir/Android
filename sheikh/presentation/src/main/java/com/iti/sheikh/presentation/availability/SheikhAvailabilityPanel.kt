package com.iti.sheikh.presentation.availability

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            is AvailabilityEffect.NavigateToCall -> onMeetingAccepted(
                effect.requestId,
                effect.token,
                effect.channelName,
                effect.userAccount,
            )

            is AvailabilityEffect.ShowMessage -> Unit
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onIntent(AvailabilityIntent.ToggleAvailability(false)) }
    }

    SheikhAvailabilityContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
