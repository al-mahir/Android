package com.iti.presentation.meetingrequest.request

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.meetingrequest.request.RequestEffect
import com.iti.presentation.meetingrequest.request.RequestIntent
import org.koin.androidx.compose.koinViewModel

@Composable
fun MeetingRequestScreen(
    sheikhId: String,
    onBack: () -> Unit,
    onMeetingAccepted: (String, String, String, Int) -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: MeetingRequestViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is RequestEffect.MeetingAccepted -> onMeetingAccepted(effect.circleId, effect.agoraToken, effect.channelName, effect.uid)
            is RequestEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    MeetingRequestContent(
        state = state,
        onSend = { note -> viewModel.onIntent(RequestIntent.Send(sheikhId, note)) },
        onCancel = { viewModel.onIntent(RequestIntent.Cancel) },
        onBack = onBack,
    )
}


