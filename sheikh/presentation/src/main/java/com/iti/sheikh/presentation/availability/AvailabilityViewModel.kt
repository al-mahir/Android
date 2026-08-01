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
import com.iti.meeting.domain.repository.MeetingRequestEvent
import com.iti.sheikh.presentation.availability.state.AvailabilityEffect
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
    private var activeCallJob: Job? = null
    private var mySheikhId: String? = null
    private var activeRequestId: String? = null

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
        heartbeat.start()

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

        repository.reconnected
            .onEach { repository.getSheikhAvailability(sheikhId) }
            .launchIn(viewModelScope)

        updateState { AvailabilityUiState.Available }
    }

    private fun goOffline() {
        heartbeat.stop()
        countdownJob?.cancel()
        activeCallJob?.cancel()
        updateState { AvailabilityUiState.Offline }
    }

    private fun startCountdown(expiresAtString: String) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val expiresAt = runCatching { 
                val str = if (expiresAtString.endsWith("Z") || expiresAtString.contains("+")) expiresAtString else "${expiresAtString}Z"
                java.time.Instant.parse(str) 
            }.getOrNull() ?: java.time.Instant.now()
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
        android.util.Log.d(TAG, "accept: calling acceptMeetingRequest(${requestState.requestId})")
        repository.acceptMeetingRequest(requestState.requestId)
            .onSuccess { accepted ->
                android.util.Log.d(TAG, "accept: SUCCESS requestId=${accepted.requestId} channel=${accepted.channelName} tokenLen=${accepted.agoraToken.length}")
                activeRequestId = accepted.requestId
                updateState {
                    AvailabilityUiState.Busy(
                        requestId = accepted.requestId,
                        token = accepted.agoraToken,
                        channelName = accepted.channelName,
                        userAccount = accepted.userAccount,
                        remoteDisplayName = requestState.studentName,
                    )
                }
                heartbeat.pause()
                observeActiveCall(accepted.requestId)
            }
            .onFailure { error ->
                android.util.Log.e(TAG, "accept: FAILED", error)
                sendEffect(AvailabilityEffect.ShowMessage(error.message ?: "Failed to accept"))
            }
    }

    /** Watches the accepted request's topic so the panel resets to Available if the call ends remotely. */
    private fun observeActiveCall(requestId: String) {
        android.util.Log.d(TAG, "observeActiveCall: subscribing requestId=$requestId")
        activeCallJob?.cancel()
        activeCallJob = repository.observeMeetingRequestEvents(requestId)
            .onEach { event ->
                android.util.Log.d(TAG, "observeActiveCall: event=$event for requestId=$requestId")
                if (event is MeetingRequestEvent.MeetingEnded) {
                    activeRequestId = null
                    heartbeat.start()
                    updateState { if (this is AvailabilityUiState.Busy) AvailabilityUiState.Available else this }
                    android.util.Log.d(TAG, "observeActiveCall: MeetingEnded -> reset to Available, currentState=$currentState")
                }
            }
            .launchIn(viewModelScope)
    }

    private fun decline() = viewModelScope.launch {
        val requestState = currentState as? AvailabilityUiState.IncomingRequest ?: return@launch
        countdownJob?.cancel()
        repository.declineMeetingRequest(requestState.requestId)
        updateState { AvailabilityUiState.Available }
    }

    override fun onCleared() {
        super.onCleared()
        if (mySheikhId != null) heartbeat.stop()
    }

    private companion object {
        const val COUNTDOWN_TICK_MS = 1_000L
        const val TAG = "MeetingLifecycle"
    }
}
