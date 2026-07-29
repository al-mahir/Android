package com.iti.sheikh.presentation.availability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.MeetingCurrentUserProvider
import com.iti.sheikh.presentation.core.mvi.DefaultEffectPublisher
import com.iti.sheikh.presentation.core.mvi.DefaultStateHolder
import com.iti.sheikh.presentation.core.mvi.EffectPublisher
import com.iti.sheikh.presentation.core.mvi.StateHolder
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.meeting.domain.repository.IncomingRequestEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AvailabilityViewModel(
    private val repository: MeetingRepository,
    private val currentUserProvider: MeetingCurrentUserProvider,
) : ViewModel(),
    StateHolder<AvailabilityUiState> by DefaultStateHolder(AvailabilityUiState.Offline),
    EffectPublisher<AvailabilityEffect> by DefaultEffectPublisher() {

    private val heartbeat = AvailabilityHeartbeat(repository, viewModelScope)
    private var countdownJob: Job? = null
    private var mySheikhId: String? = null

    fun onIntent(intent: AvailabilityIntent) {
        when (intent) {
            is AvailabilityIntent.ToggleAvailability ->
                if (intent.isAvailable) goAvailable() else goOffline()
            AvailabilityIntent.Accept -> accept()
            AvailabilityIntent.Decline -> decline()
        }
    }

    private fun goAvailable() = viewModelScope.launch {
        val sheikhId = currentUserProvider.currentUserId() ?: return@launch
        mySheikhId = sheikhId
        heartbeat.start(sheikhId)
        
        repository.observeIncomingRequests(sheikhId)
            .onEach { event ->
                when (event) {
                    is IncomingRequestEvent.Received -> {
                        updateState {
                            AvailabilityUiState.IncomingRequest(
                                requestId = event.requestId,
                                studentName = event.studentName,
                                note = event.note,
                                expiresAt = event.expiresAt,
                            )
                        }
                        startCountdown(event.expiresAt)
                    }
                    is IncomingRequestEvent.Cancelled -> {
                        countdownJob?.cancel()
                        updateState { AvailabilityUiState.Available }
                    }
                }
            }
            .launchIn(viewModelScope)
            
        updateState { AvailabilityUiState.Available }
    }

    private fun goOffline() {
        val sheikhId = mySheikhId ?: return
        heartbeat.stop(sheikhId)
        countdownJob?.cancel()
        updateState { AvailabilityUiState.Offline }
    }

    private fun startCountdown(expiresAtString: String) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val expiresAt = runCatching { java.time.Instant.parse(expiresAtString) }.getOrNull() ?: java.time.Instant.now()
            while (isActive) {
                if (expiresAt.isBefore(java.time.Instant.now()) || expiresAt == java.time.Instant.now()) {
                    updateState {
                        if (this is AvailabilityUiState.IncomingRequest) AvailabilityUiState.Available else this
                    }
                    break
                }
                delay(COUNTDOWN_TICK_MS)
            }
        }
    }

    private fun accept() = viewModelScope.launch {
        val requestState = currentState as? AvailabilityUiState.IncomingRequest ?: return@launch
        countdownJob?.cancel()
        repository.acceptMeetingRequest(requestState.requestId)
            .onSuccess { accepted ->
                updateState { AvailabilityUiState.Busy }
                sendEffect(
                    AvailabilityEffect.NavigateToCall(
                        circleId = accepted.circleId,
                        token = accepted.sheikhAgoraToken,
                        channelName = accepted.channelName,
                        uid = accepted.uid,
                    ),
                )
            }
            .onFailure { error ->
                sendEffect(AvailabilityEffect.ShowMessage(error.message ?: "Failed to accept"))
            }
    }

    private fun decline() = viewModelScope.launch {
        val requestState = currentState as? AvailabilityUiState.IncomingRequest ?: return@launch
        countdownJob?.cancel()
        repository.declineMeetingRequest(requestState.requestId, reason = null)
        updateState { AvailabilityUiState.Available }
    }

    override fun onCleared() {
        super.onCleared()
        mySheikhId?.let { heartbeat.stop(it) }
    }

    private companion object {
        const val COUNTDOWN_TICK_MS = 1_000L
    }
}




