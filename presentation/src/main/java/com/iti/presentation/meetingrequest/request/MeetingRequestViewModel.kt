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

    /** The locally-cached pending request, if any, scoped to [sheikhId] — used by
     * [MeetingRequestScreen] to decide whether to rehydrate into [RequestUiState.Pending] on
     * first composition instead of showing the blank Send form. */
    suspend fun currentPendingRequestFor(sheikhId: String) =
        repository.getPendingRequest()?.takeIf { it.sheikhId == sheikhId }

    fun onIntent(intent: RequestIntent) {
        when (intent) {
            is RequestIntent.Send -> send(intent.sheikhId, intent.sheikhName, intent.note)
            RequestIntent.Cancel -> cancel()
            RequestIntent.CancelExisting -> cancelExisting()
            RequestIntent.Reset -> reset()
            is RequestIntent.Resume -> resume(intent.requestId, intent.expiresAt)
        }
    }

    private fun send(sheikhId: String, sheikhName: String?, note: String?) = viewModelScope.launch {
        updateState { RequestUiState.Sending }
        when (val result = repository.sendMeetingRequest(sheikhId, sheikhName, note)) {
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

            is SendMeetingRequestResult.AlreadyPending -> {
                updateState { RequestUiState.AlreadyPending(result.message) }
            }

            is SendMeetingRequestResult.Error -> {
                updateState { RequestUiState.Idle }
                sendEffect(RequestEffect.ShowMessage(result.message))
            }
        }
    }

    /** Cancels the pre-existing request that blocked [send] (surfaced via
     * [RequestUiState.AlreadyPending]), then returns to Idle so the student can retry. */
    private fun cancelExisting() = viewModelScope.launch {
        val existing = repository.getPendingRequest()
        if (existing != null) {
            repository.cancelMeetingRequest(existing.requestId)
        }
        updateState { RequestUiState.Idle }
    }

    private fun reset() {
        eventsJob?.cancel()
        countdownJob?.cancel()
        updateState { RequestUiState.Idle }
    }

    /** Rehydrates an in-flight request without re-POSTing — used when the student navigates in
     * from the Home pending-request banner rather than the initial Send action. */
    private fun resume(requestId: String, expiresAt: String) {
        updateState { RequestUiState.Pending(requestId, expiresAt) }
        subscribeToRequest(requestId)
        startCountdown(expiresAt)
    }

    private fun subscribeToRequest(requestId: String) {
        eventsJob?.cancel()
        eventsJob = repository.observeMeetingRequestEvents(requestId)
            .onEach { event ->
                when (event) {
                    is MeetingRequestEvent.Accepted -> {
                        countdownJob?.cancel()
                        // No navigation effect here — MeetingRequestScreen drives navigation off
                        // this Accepted state (LaunchedEffect keyed on requestId), not a one-shot
                        // effect Channel, since a StateFlow reliably replays to any (re)started
                        // collector where a Channel would not — see the sheikh-side
                        // AvailabilityViewModel.accept() for the same fix, applied first there
                        // after the exact same symptom (stuck on Home for a second accept).
                        updateState {
                            RequestUiState.Accepted(requestId, event.agoraToken, event.channelName, event.userAccount)
                        }
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
