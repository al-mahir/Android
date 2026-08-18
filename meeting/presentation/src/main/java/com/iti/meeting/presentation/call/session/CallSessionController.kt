package com.iti.meeting.presentation.call.session

import android.content.Context
import android.util.Log
import com.iti.meeting.domain.config.MeetingKitConfig
import com.iti.meeting.domain.model.ActiveCallRecord
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.meeting.domain.repository.MeetingRequestEvent
import com.iti.meeting.presentation.agora.AgoraEngineWrapper
import com.iti.meeting.presentation.call.audio.AudioOutputDevice
import com.iti.meeting.presentation.call.audio.AudioRouteController
import com.iti.meeting.presentation.call.state.CallErrorReason
import com.iti.meeting.presentation.call.state.CallUiState
import com.iti.meeting.presentation.core.mvi.DefaultStateHolder
import com.iti.meeting.presentation.core.mvi.StateHolder
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "MeetingCall"

class CallSessionController(
    private val appContext: Context,
    private val config: MeetingKitConfig,
    private val repository: MeetingRepository,
    private val audioRoutes: AudioRouteController,
    private val ongoingCalls: OngoingCallRegistry,
) : StateHolder<CallSessionState> by DefaultStateHolder(CallSessionState()) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var engine: AgoraEngineWrapper? = null
        private set

    private var durationJob: Job? = null
    private var eventsJob: Job? = null
    private var joinTimeoutJob: Job? = null
    private var audioRoutesJob: Job? = null

    private fun updateCallState(transform: CallUiState.() -> CallUiState) {
        updateState { copy(callState = callState.transform()) }
    }

    private inline fun updateInCall(crossinline transform: CallUiState.InCall.() -> CallUiState.InCall) {
        updateCallState { if (this is CallUiState.InCall) transform() else this }
    }

        fun prepareForRequest(newRequestId: String) {
        val requestId = currentState.requestId
        Log.d(TAG, "prepareForRequest: newRequestId=$newRequestId, this.requestId=$requestId, currentState=${currentState.callState}")
        if (requestId != null && requestId != newRequestId) {
            Log.w(TAG, "prepareForRequest: stale state from previous call $requestId, resetting for $newRequestId")
            teardown()
        }
    }

    fun joinChannel(
        context: Context,
        requestId: String,
        token: String,
        channelName: String,
        userAccount: String,
        remoteDisplayName: String?,
        micEnabled: Boolean,
        cameraEnabled: Boolean,
    ) {
        val existingRequestId = currentState.requestId
        Log.d(TAG, "joinChannel: called requestId=$requestId, this.requestId=$existingRequestId, engine=$engine, joinTimeoutJob=$joinTimeoutJob")
        if (existingRequestId != null && existingRequestId != requestId) {
            Log.w(TAG, "joinChannel: stale state from previous call $existingRequestId, tearing down before joining $requestId")
            teardown()
        }
        if (engine != null || joinTimeoutJob != null) {
            Log.d(TAG, "joinChannel: already joined/joining requestId=$requestId, skipping")
            return
        }
        ongoingCalls.start(
            context = appContext,
            display = OngoingCallDisplay(title = remoteDisplayName, isMicEnabled = micEnabled, isConnecting = true),
            actions = OngoingCallActions(onToggleMic = ::toggleMic, onHangUp = ::endCall),
        )
        updateState {
            copy(
                requestId = requestId,
                channelName = channelName,
                userAccount = userAccount,
                remoteDisplayName = remoteDisplayName,
                callState = CallUiState.Connecting,
            )
        }
        observeMeetingEnded(requestId)
        startAudioRouteTracking()

        scope.launch {
            repository.refreshToken(requestId)
                .onSuccess { refreshed ->
                    Log.d(TAG, "refreshToken: SUCCESS requestId=$requestId channel=${refreshed.channelName} tokenLen=${refreshed.token.length}")
                    startAgoraJoin(context, refreshed.token, refreshed.channelName, refreshed.userAccount, micEnabled, cameraEnabled)
                }
                .onFailure {
                    Log.w(TAG, "refreshToken failed before join, falling back to the passed-in token", it)
                    startAgoraJoin(context, token, channelName, userAccount, micEnabled, cameraEnabled)
                }
        }
    }

    private fun releaseEngine() {
        durationJob?.cancel()
        durationJob = null
        joinTimeoutJob?.cancel()
        joinTimeoutJob = null
        audioRoutesJob?.cancel()
        audioRoutesJob = null
        audioRoutes.stop()
        engine?.leaveChannel()
        engine?.destroy()
        engine = null
    }

    private fun teardown() {
        releaseEngine()
        eventsJob?.cancel()
        eventsJob = null
        ongoingCalls.stop(appContext)
        updateState { CallSessionState() }
    }

    /**
     * Starts watching the connected outputs and pushes every change into both the engine and the
     * UI state. Started at [joinChannel] rather than after the join succeeds so a headset plugged
     * in while "Connecting…" is showing is already the selected route by the time audio starts.
     */
    private fun startAudioRouteTracking() {
        audioRoutesJob?.cancel()
        audioRoutes.start()
        audioRoutesJob = audioRoutes.state
            .onEach { routeState ->
                engine?.setAudioRoute(routeState.selected.agoraRoute)
                updateInCall {
                    copy(audioDevice = routeState.selected, availableAudioDevices = routeState.available)
                }
            }
            .launchIn(scope)
    }

    private fun startAgoraJoin(
        context: Context,
        token: String,
        channelName: String,
        userAccount: String,
        micEnabled: Boolean,
        cameraEnabled: Boolean,
    ) {
        if (engine != null) return

        Log.i(TAG, "joinChannel: channel=$channelName userAccount=$userAccount appIdLen=${config.agoraAppId.length} tokenLen=${token.length} mic=$micEnabled cam=$cameraEnabled")

        joinTimeoutJob = scope.launch {
            delay(JOIN_TIMEOUT_MS)
            if (currentState.callState is CallUiState.Connecting) {
                updateCallState { CallUiState.Error(CallErrorReason.CONNECT_FAILED) }
            }
        }

        val eventHandler = object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, joinedUid: Int, elapsed: Int) {
                Log.i(TAG, "onJoinChannelSuccess: channel=$channel uid=$joinedUid elapsed=${elapsed}ms")
                joinTimeoutJob?.cancel()
                joinTimeoutJob = null
                val routeState = audioRoutes.state.value
                updateCallState {
                    if (this is CallUiState.InCall) this
                    else CallUiState.InCall(
                        remoteUid = null,
                        isMicEnabled = micEnabled,
                        isCameraEnabled = cameraEnabled,
                        audioDevice = routeState.selected,
                        availableAudioDevices = routeState.available,
                        isTorchAvailable = engine?.isTorchSupported() ?: false,
                    )
                }
                applyAudioRoute(routeState.selected)
                startDurationTimer()
                persistActiveCall()
                publishOngoingCall()
            }

            override fun onUserJoined(remoteUid: Int, elapsed: Int) {
                Log.i(TAG, "onUserJoined: remoteUid=$remoteUid elapsed=${elapsed}ms")
                val eventUid = remoteUid
                updateCallState {
                    if (this is CallUiState.InCall) copy(remoteUid = eventUid)
                    else CallUiState.InCall(remoteUid = eventUid, isMicEnabled = micEnabled, isCameraEnabled = cameraEnabled)
                }
            }

            override fun onUserOffline(remoteUid: Int, reason: Int) {
                Log.i(TAG, "onUserOffline: remoteUid=$remoteUid reason=$reason")
                updateInCall { copy(remoteUid = null, isRemoteMicEnabled = true, isRemoteCameraEnabled = true) }
            }

            override fun onUserMuteAudio(remoteUid: Int, muted: Boolean) {
                Log.i(TAG, "onUserMuteAudio: remoteUid=$remoteUid muted=$muted")
                val eventUid = remoteUid
                updateInCall { if (eventUid == this.remoteUid) copy(isRemoteMicEnabled = !muted) else this }
            }

            override fun onUserMuteVideo(remoteUid: Int, muted: Boolean) {
                Log.i(TAG, "onUserMuteVideo: remoteUid=$remoteUid muted=$muted")
                val eventUid = remoteUid
                updateInCall { if (eventUid == this.remoteUid) copy(isRemoteCameraEnabled = !muted) else this }
            }

              override fun onAudioRouteChanged(routing: Int) {
                Log.i(TAG, "onAudioRouteChanged: routing=$routing")
                AudioOutputDevice.fromAgoraRoute(routing)?.let(audioRoutes::onEngineReportedRoute)
            }

            override fun onConnectionStateChanged(state: Int, reason: Int) {
                Log.i(TAG, "onConnectionStateChanged: state=$state reason=$reason")
                when (state) {
                    Constants.CONNECTION_STATE_RECONNECTING -> updateInCall { copy(isReconnecting = true) }
                    Constants.CONNECTION_STATE_CONNECTED -> updateInCall { copy(isReconnecting = false) }
                    Constants.CONNECTION_STATE_FAILED -> {
                        joinTimeoutJob?.cancel()
                        joinTimeoutJob = null
                        updateCallState { CallUiState.Error(CallErrorReason.CONNECTION_LOST) }
                    }
                }
            }

            override fun onError(err: Int) {
                Log.e(TAG, "onError: code=$err")
                joinTimeoutJob?.cancel()
                joinTimeoutJob = null
                updateCallState { CallUiState.Error(CallErrorReason.ENGINE_ERROR, agoraErrorCode = err) }
            }

            override fun onTokenPrivilegeWillExpire(token: String?) {
                Log.w(TAG, "onTokenPrivilegeWillExpire")
                renewToken()
            }

            override fun onRequestToken() {
                Log.w(TAG, "onRequestToken: engine is requesting a fresh token, current one was rejected/expired")
                renewToken()
            }

            override fun onConnectionLost() {
                Log.w(TAG, "onConnectionLost")
            }

            override fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats?) {
                Log.i(TAG, "onLeaveChannel")
            }
        }

        val wrapper = AgoraEngineWrapper(context.applicationContext, config.agoraAppId, eventHandler)
        engine = wrapper
        wrapper.setCameraDirection(front = true)
        wrapper.joinChannel(token, channelName, userAccount, publishAudio = micEnabled, publishVideo = cameraEnabled)
    }

    /**
     * Applies [device] to the engine, then re-applies it once shortly after.
     *
     * The repeat is not superstition: several OEM audio HALs finish setting up the voice-call
     * stream a beat after `onJoinChannelSuccess`, and reset the route while doing so — which is the
     * intermittent half of "sometimes it comes out of the earpiece". Re-asserting once the session
     * has settled makes the outcome deterministic.
     */
    private fun applyAudioRoute(device: AudioOutputDevice) {
        engine?.setAudioRoute(device.agoraRoute)
        scope.launch {
            delay(ROUTE_REASSERT_DELAY_MS)
            if (currentState.callState is CallUiState.InCall) {
                engine?.setAudioRoute(audioRoutes.currentDevice.agoraRoute)
            }
        }
    }

    private fun observeMeetingEnded(requestId: String) {
        eventsJob?.cancel()
        eventsJob = repository.observeMeetingRequestEvents(requestId)
            .onEach { event ->
                if (event is MeetingRequestEvent.MeetingEnded) {
                    // Remote hangup/timeout: release the engine now rather than leaving it
                    // attached to a dead channel until the next call's stale-state teardown, but
                    // keep surfacing `requestId`/`Ended` so a composed CallScreen's own
                    // `state is Ended` watcher still fires and navigates away.
                    releaseEngine()
                    ongoingCalls.stop(appContext)
                    updateCallState { CallUiState.Ended }
                }
            }
            .launchIn(scope)
    }

    private fun renewToken() {
        val id = currentState.requestId ?: return
        scope.launch {
            repository.refreshToken(id).onSuccess { refreshed ->
                engine?.renewToken(refreshed.token)
            }
        }
    }

    fun endCall() {
        val id = currentState.requestId
        if (id != null) scope.launch {
            endMeetingReliably(id)
            repository.clearActiveCall()
        }
        releaseEngine()
        eventsJob?.cancel()
        eventsJob = null
        ongoingCalls.stop(appContext)
        updateCallState { if (this is CallUiState.Ended) this else CallUiState.Ended }
    }

    /** Re-renders the ongoing-call notification from the current session state. */
    private fun publishOngoingCall() {
        val session = currentState
        ongoingCalls.update(
            OngoingCallDisplay(
                title = session.remoteDisplayName,
                isMicEnabled = (session.callState as? CallUiState.InCall)?.isMicEnabled ?: false,
                isConnecting = session.callState is CallUiState.Connecting,
            )
        )
    }

    private suspend fun endMeetingReliably(id: String) {
        repository.endMeeting(id)
            .onSuccess { Log.d(TAG, "endCall: endMeeting($id) SUCCESS") }
            .onFailure { error ->
                Log.w(TAG, "endCall: endMeeting($id) failed, retrying once", error)
                repository.endMeeting(id)
                    .onSuccess { Log.d(TAG, "endCall: endMeeting($id) retry SUCCESS") }
                    .onFailure {
                        Log.e(TAG, "endCall: endMeeting($id) failed again — backend was never told this call ended, sheikh may be stuck BUSY server-side", it)
                    }
            }
    }

    private fun persistActiveCall() {
        val session = currentState
        val requestId = session.requestId ?: return
        val channelName = session.channelName ?: return
        val userAccount = session.userAccount ?: return
        scope.launch {
            repository.saveActiveCall(
                ActiveCallRecord(
                    requestId = requestId,
                    channelName = channelName,
                    userAccount = userAccount,
                    remoteDisplayName = session.remoteDisplayName,
                ),
            )
        }
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (true) {
                delay(1_000)
                updateInCall { copy(callDurationSeconds = callDurationSeconds + 1) }
            }
        }
    }

    fun toggleMic() {
        val current = currentState.callState as? CallUiState.InCall ?: return
        val newState = !current.isMicEnabled
        engine?.setLocalAudioEnabled(newState)
        updateInCall { copy(isMicEnabled = newState) }
        publishOngoingCall()
    }

    fun toggleCamera() {
        val current = currentState.callState as? CallUiState.InCall ?: return
        val newState = !current.isCameraEnabled
        engine?.setLocalVideoEnabled(newState)
        updateInCall {
            copy(
                isCameraEnabled = newState,
                isTorchAvailable = if (newState) isTorchSupportedNow() else isTorchAvailable,
                isTorchOn = if (newState) isTorchOn else false,
            )
        }
    }

    private fun isTorchSupportedNow(): Boolean = engine?.isTorchSupported() ?: false

    /** Explicit output pick from the in-call picker. The engine call happens in the collector set
     * up by [startAudioRouteTracking], so this stays the single path for route changes. */
    fun selectAudioDevice(device: AudioOutputDevice) = audioRoutes.select(device)

    fun setCameraFacing(front: Boolean) {
        val current = currentState.callState as? CallUiState.InCall ?: return
        if (current.isFrontCamera == front) return
        engine?.setCameraDirection(front)
        updateInCall {
            copy(
                isFrontCamera = front,
                // Torch support is per-camera, so it has to be re-probed on every switch.
                isTorchAvailable = if (isCameraEnabled) isTorchSupportedNow() else isTorchAvailable,
                // Front cameras have no torch; leaving `isTorchOn` set would show a lit control
                // over a camera that can't light anything.
                isTorchOn = if (front) false else isTorchOn,
            )
        }
    }

    fun switchCamera() {
        val current = currentState.callState as? CallUiState.InCall ?: return
        setCameraFacing(front = !current.isFrontCamera)
    }

    fun toggleTorch() {
        val current = currentState.callState as? CallUiState.InCall ?: return
        if (!current.isTorchAvailable || current.isFrontCamera) return
        val newState = !current.isTorchOn
        engine?.setTorchEnabled(newState)
        updateInCall { copy(isTorchOn = newState) }
    }

    private companion object {
        const val JOIN_TIMEOUT_MS = 15_000L
        const val ROUTE_REASSERT_DELAY_MS = 1_200L
    }
}
