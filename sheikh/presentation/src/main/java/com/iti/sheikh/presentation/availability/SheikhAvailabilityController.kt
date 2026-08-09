package com.iti.sheikh.presentation.availability

import android.content.Context
import android.util.Log
import com.iti.domain.auth.MeetingCurrentUserProvider
import com.iti.meeting.domain.repository.IncomingRequestEvent
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.meeting.domain.repository.MeetingRequestEvent
import com.iti.sheikh.presentation.availability.service.SheikhAvailabilityForegroundService
import com.iti.sheikh.presentation.core.mvi.DefaultStateHolder
import com.iti.sheikh.presentation.core.mvi.StateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Owns sheikh availability (heartbeat + incoming-request STOMP subscription + accept/decline) on
 * a process-lifetime scope instead of a ViewModel's — so it keeps listening for incoming requests
 * whether or not [com.iti.sheikh.presentation.availability.SheikhAvailabilityPanel] is composed,
 * mirroring how [com.iti.meeting.presentation.call.session.CallSessionController] owns an active
 * call independent of any screen. Started/observed by [SheikhAvailabilityForegroundService].
 */
class SheikhAvailabilityController(
    private val appContext: Context,
    private val repository: MeetingRepository,
    private val currentUserProvider: MeetingCurrentUserProvider,
) : StateHolder<AvailabilityUiState> by DefaultStateHolder(AvailabilityUiState.Offline) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val heartbeat = AvailabilityHeartbeat(repository, scope)
    private var countdownJob: Job? = null
    private var activeCallJob: Job? = null
    private var isAccepting = false

    // This controller is a process-lifetime singleton (unlike the old ViewModel it replaced,
    // which was recreated — and thus implicitly re-guarded against double-subscribing — every
    // time Home was recomposed). Without this guard, `goAvailable()` being invoked more than once
    // over the process's life would stack a second, independent `observeIncomingRequests`
    // collector on top of the first, so every event gets processed twice (the visible symptom:
    // a ringing notification "for a request that already arrived" and worse.
    private var isSubscribed = false

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors

    fun goAvailable() = scope.launch {
        val sheikhId = currentUserProvider.currentUserId() ?: return@launch
        heartbeat.start()
            .onFailure { error ->
                Log.e(TAG, "goAvailable: setMyAvailability(AVAILABLE) failed, staying Offline", error)
                _errors.tryEmit(error.message ?: "Couldn't go available. Please try again.")
                return@launch
            }
        SheikhAvailabilityForegroundService.start(appContext)

        if (isSubscribed) {
            updateState { AvailabilityUiState.Available }
            return@launch
        }
        isSubscribed = true

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
            .launchIn(scope)

        repository.reconnected
            .onEach { repository.getSheikhAvailability(sheikhId) }
            .launchIn(scope)

        updateState { AvailabilityUiState.Available }
    }

    fun goOffline() {
        heartbeat.stop()
        countdownJob?.cancel()
        activeCallJob?.cancel()
        updateState { AvailabilityUiState.Offline }
    }

    private fun startCountdown(expiresAtString: String) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            val expiresAt = runCatching {
                val str = if (expiresAtString.endsWith("Z") || expiresAtString.contains("+")) expiresAtString else "${expiresAtString}Z"
                Instant.parse(str)
            }.getOrNull() ?: Clock.System.now()
            while (isActive) {
                if (expiresAt <= Clock.System.now()) {
                    updateState {
                        if (this is AvailabilityUiState.IncomingRequest) AvailabilityUiState.Available else this
                    }
                    break
                }
                delay(COUNTDOWN_TICK_MS)
            }
        }
    }

    fun accept(requestId: String) = scope.launch {
        if (isAccepting) {
            Log.d(TAG, "accept: already accepting, ignoring duplicate call for requestId=$requestId")
            return@launch
        }
        val requestState = currentState as? AvailabilityUiState.IncomingRequest ?: return@launch
        if (requestState.requestId != requestId) return@launch
        isAccepting = true
        countdownJob?.cancel()
        Log.d(TAG, "accept: calling acceptMeetingRequest($requestId)")
        repository.acceptMeetingRequest(requestId)
            .onSuccess { accepted ->
                Log.d(TAG, "accept: SUCCESS requestId=${accepted.requestId} channel=${accepted.channelName} tokenLen=${accepted.agoraToken.length}")
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
                Log.e(TAG, "accept: FAILED", error)
                _errors.tryEmit(error.message ?: "Failed to accept")
            }
        isAccepting = false
    }

    /** Watches the accepted request's topic so the panel resets to Available if the call ends remotely. */
    private fun observeActiveCall(requestId: String) {
        Log.d(TAG, "observeActiveCall: subscribing requestId=$requestId")
        activeCallJob?.cancel()
        activeCallJob = repository.observeMeetingRequestEvents(requestId)
            .onEach { event ->
                Log.d(TAG, "observeActiveCall: event=$event for requestId=$requestId")
                if (event is MeetingRequestEvent.MeetingEnded) {
                    heartbeat.start().onFailure { error ->
                        Log.e(TAG, "observeActiveCall: resuming heartbeat failed", error)
                    }
                    updateState { if (this is AvailabilityUiState.Busy) AvailabilityUiState.Available else this }
                    Log.d(TAG, "observeActiveCall: MeetingEnded -> reset to Available, currentState=$currentState")
                }
            }
            .launchIn(scope)
    }

    fun decline(requestId: String) = scope.launch {
        val requestState = currentState as? AvailabilityUiState.IncomingRequest ?: return@launch
        if (requestState.requestId != requestId) return@launch
        countdownJob?.cancel()
        repository.declineMeetingRequest(requestId)
        updateState { AvailabilityUiState.Available }
    }

    private companion object {
        const val COUNTDOWN_TICK_MS = 1_000L
        const val TAG = "MeetingLifecycle"
    }
}
