package com.iti.presentation.meetingrequest.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.meeting.domain.repository.SendMeetingRequestResult
import com.iti.meeting.domain.repository.MeetingRequestEvent
import com.iti.presentation.meetingrequest.request.RequestEffect
import com.iti.presentation.meetingrequest.request.RequestIntent
import com.iti.presentation.meetingrequest.request.RequestUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MeetingRequestViewModel(
    private val repository: MeetingRepository,
) : ViewModel(),
    StateHolder<RequestUiState> by DefaultStateHolder(RequestUiState.Idle),
    EffectPublisher<RequestEffect> by DefaultEffectPublisher() {

    private var countdownJob: Job? = null
    private var eventsJob: Job? = null

    fun onIntent(intent: RequestIntent) {
        when (intent) {
            is RequestIntent.Send -> send(intent.sheikhId, intent.note)
            RequestIntent.Cancel -> cancel()
        }
    }

    private fun send(sheikhId: String, note: String?) = viewModelScope.launch {
        updateState { RequestUiState.Sending }
        when (val result = repository.sendMeetingRequest(sheikhId, note)) {
            is SendMeetingRequestResult.Pending -> {
                updateState { RequestUiState.Pending(result.requestId, result.expiresAt) }
                subscribeToRequest(result.requestId)
                startCountdown(result.expiresAt)
            }

            SendMeetingRequestResult.SheikhUnavailable -> {
                updateState { RequestUiState.Idle }
                sendEffect(RequestEffect.ShowMessage("This sheikh is no longer available."))
            }

            SendMeetingRequestResult.SheikhNotFound -> {
                updateState { RequestUiState.Idle }
                sendEffect(RequestEffect.ShowMessage("Sheikh not found."))
            }

            is SendMeetingRequestResult.Error -> {
                updateState { RequestUiState.Idle }
                sendEffect(RequestEffect.ShowMessage(result.message))
            }
        }
    }

    private fun subscribeToRequest(requestId: String) {
        eventsJob?.cancel()
        eventsJob = repository.observeMeetingRequestEvents(requestId)
            .onEach { event ->
                when (event) {
                    is MeetingRequestEvent.Accepted -> {
                        countdownJob?.cancel()
                        updateState {
                            RequestUiState.Accepted(requestId, event.agoraToken, event.channelName, event.userAccount)
                        }
                        sendEffect(RequestEffect.MeetingAccepted(requestId, event.agoraToken, event.channelName, event.userAccount))
                    }
                    is MeetingRequestEvent.Declined -> {
                        countdownJob?.cancel()
                        updateState { RequestUiState.Declined(event.reason) }
                    }
                    MeetingRequestEvent.Expired -> {
                        countdownJob?.cancel()
                        updateState { RequestUiState.Expired }
                    }
                    MeetingRequestEvent.Cancelled -> Unit // this student already left Pending locally via the Cancel intent
                    MeetingRequestEvent.MeetingEnded -> {
                        updateState { if (this is RequestUiState.Accepted) RequestUiState.Ended else this }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startCountdown(expiresAtString: String) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val fixedString = if (!expiresAtString.endsWith("Z")) "${expiresAtString}Z" else expiresAtString
            val expiresAt = runCatching { java.time.Instant.parse(fixedString) }.getOrNull() 
                ?: java.time.Instant.now().plusSeconds(60) // Safe fallback to avoid immediate expiry

            while (isActive) {
                if (expiresAt.isBefore(java.time.Instant.now()) || expiresAt == java.time.Instant.now()) {
                    updateState { if (this is RequestUiState.Pending) RequestUiState.Expired else this }
                    break
                }
                delay(1_000L)
            }
        }
    }

    private fun cancel() = viewModelScope.launch {
        val pending = currentState as? RequestUiState.Pending ?: return@launch
        countdownJob?.cancel()
        repository.cancelMeetingRequest(pending.requestId)
        updateState { RequestUiState.Idle }
    }
}
