package com.iti.presentation.meetingrequest.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.MeetingCurrentUserProvider
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
    private val currentUserProvider: MeetingCurrentUserProvider,
) : ViewModel(),
    StateHolder<RequestUiState> by DefaultStateHolder(RequestUiState.Idle),
    EffectPublisher<RequestEffect> by DefaultEffectPublisher() {

    private var countdownJob: Job? = null

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

    private fun subscribeToRequest(requestId: String) = viewModelScope.launch {
        val studentId = currentUserProvider.currentUserId() ?: return@launch
        repository.observeMeetingRequestEvents(studentId, requestId)
            .onEach { event ->
                when (event) {
                    is MeetingRequestEvent.Accepted -> {
                        countdownJob?.cancel()
                        updateState {
                            RequestUiState.Accepted(event.circleId, event.agoraToken, event.channelName, event.uid)
                        }
                        sendEffect(RequestEffect.MeetingAccepted(event.circleId, event.agoraToken, event.channelName, event.uid))
                    }
                    is MeetingRequestEvent.Declined -> {
                        countdownJob?.cancel()
                        updateState { RequestUiState.Declined(event.reason) }
                    }
                    MeetingRequestEvent.Expired -> {
                        countdownJob?.cancel()
                        updateState { RequestUiState.Expired }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startCountdown(expiresAtString: String) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val expiresAt = runCatching { java.time.Instant.parse(expiresAtString) }.getOrNull() ?: java.time.Instant.now()
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







