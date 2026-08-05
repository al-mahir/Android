package com.iti.meeting.presentation.call.session

import android.content.Context
import android.util.Log
import com.iti.meeting.domain.config.MeetingKitConfig
import com.iti.meeting.domain.model.ActiveCallRecord
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.meeting.domain.repository.MeetingRequestEvent
import com.iti.meeting.presentation.agora.AgoraEngineWrapper
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
) : StateHolder<CallSessionState> by DefaultStateHolder(CallSessionState()) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var engine: AgoraEngineWrapper? = null
        private set

    private var durationJob: Job? = null
    private var eventsJob: Job? = null
    private var joinTimeoutJob: Job? = null

    private fun updateCallState(transform: CallUiState.() -> CallUiState) {
        updateState { copy(callState = callState.transform()) }
    }

    /** Resets stale state from a previous, already-finished call before a new one is composed on
     * top of it — belt-and-suspenders alongside the same check in [joinChannel]. */
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
        CallForegroundService.start(appContext)
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

        scope.launch {
            repository.refreshToken(requestId)
                .onSuccess { refreshed ->
                    Log.d(TAG, "refreshToken: SUCCESS requestId=$requestId channel=${refreshed.channelName} tokenLen=${refreshed.token.length}")
                    startAgoraJoin(context, requestId, refreshed.token, refreshed.channelName, refreshed.userAccount, micEnabled, cameraEnabled)
                }
                .onFailure {
                    Log.w(TAG, "refreshToken failed before join, falling back to the passed-in token", it)
                    startAgoraJoin(context, requestId, token, channelName, userAccount, micEnabled, cameraEnabled)
                }
        }
    }

       private fun releaseEngine() {
        durationJob?.cancel()
        durationJob = null
        joinTimeoutJob?.cancel()
        joinTimeoutJob = null
        engine?.leaveChannel()
        engine?.destroy()
        engine = null
    }

       private fun teardown() {
        releaseEngine()
        eventsJob?.cancel()
        eventsJob = null
        updateState { CallSessionState() }
    }

    private fun startAgoraJoin(
        context: Context,
        requestId: String,
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
                updateCallState { CallUiState.Error("Couldn't connect to the call. Please try again.") }
            }
        }

        val eventHandler = object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, joinedUid: Int, elapsed: Int) {
                Log.i(TAG, "onJoinChannelSuccess: channel=$channel uid=$joinedUid elapsed=${elapsed}ms")
                joinTimeoutJob?.cancel()
                joinTimeoutJob = null
                updateCallState {
                    if (this is CallUiState.InCall) this
                    else CallUiState.InCall(remoteUid = null, isMicEnabled = micEnabled, isCameraEnabled = cameraEnabled)
                }
                startDurationTimer()
                persistActiveCall()
            }

            override fun onUserJoined(remoteUid: Int, elapsed: Int) {
                Log.i(TAG, "onUserJoined: remoteUid=$remoteUid elapsed=${elapsed}ms")
                updateCallState {
                    if (this is CallUiState.InCall) copy(remoteUid = remoteUid)
                    else CallUiState.InCall(remoteUid = remoteUid, isMicEnabled = micEnabled, isCameraEnabled = cameraEnabled)
                }
            }

            override fun onUserOffline(remoteUid: Int, reason: Int) {
                Log.i(TAG, "onUserOffline: remoteUid=$remoteUid reason=$reason")
                updateCallState {
                    if (this is CallUiState.InCall) copy(remoteUid = null, isRemoteMicEnabled = true, isRemoteCameraEnabled = true)
                    else this
                }
            }

            override fun onUserMuteAudio(remoteUid: Int, muted: Boolean) {
                Log.i(TAG, "onUserMuteAudio: remoteUid=$remoteUid muted=$muted")
                updateCallState {
                    if (this is CallUiState.InCall && remoteUid == this.remoteUid) copy(isRemoteMicEnabled = !muted) else this
                }
            }

            override fun onUserMuteVideo(remoteUid: Int, muted: Boolean) {
                Log.i(TAG, "onUserMuteVideo: remoteUid=$remoteUid muted=$muted")
                updateCallState {
                    if (this is CallUiState.InCall && remoteUid == this.remoteUid) copy(isRemoteCameraEnabled = !muted) else this
                }
            }

            override fun onConnectionStateChanged(state: Int, reason: Int) {
                Log.i(TAG, "onConnectionStateChanged: state=$state reason=$reason")
                when (state) {
                    Constants.CONNECTION_STATE_RECONNECTING ->
                        updateCallState { if (this is CallUiState.InCall) copy(isReconnecting = true) else this }
                    Constants.CONNECTION_STATE_CONNECTED ->
                        updateCallState { if (this is CallUiState.InCall) copy(isReconnecting = false) else this }
                    Constants.CONNECTION_STATE_FAILED -> {
                        joinTimeoutJob?.cancel()
                        joinTimeoutJob = null
                        updateCallState { CallUiState.Error("Connection lost. Please try again.") }
                    }
                }
            }

            override fun onError(err: Int) {
                Log.e(TAG, "onError: code=$err")
                joinTimeoutJob?.cancel()
                joinTimeoutJob = null
                updateCallState { CallUiState.Error("Call error ($err)") }
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
        wrapper.joinChannel(token, channelName, userAccount, publishAudio = micEnabled, publishVideo = cameraEnabled)
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
        updateCallState { if (this is CallUiState.Ended) this else CallUiState.Ended }
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
                updateCallState { if (this is CallUiState.InCall) copy(callDurationSeconds = callDurationSeconds + 1) else this }
            }
        }
    }

    fun toggleMic() {
        val current = currentState.callState as? CallUiState.InCall ?: return
        val newState = !current.isMicEnabled
        engine?.setLocalAudioEnabled(newState)
        updateCallState { if (this is CallUiState.InCall) copy(isMicEnabled = newState) else this }
    }

    fun toggleCamera() {
        val current = currentState.callState as? CallUiState.InCall ?: return
        val newState = !current.isCameraEnabled
        engine?.setLocalVideoEnabled(newState)
        updateCallState { if (this is CallUiState.InCall) copy(isCameraEnabled = newState) else this }
    }

    fun toggleSpeaker() {
        val current = currentState.callState as? CallUiState.InCall ?: return
        val newState = !current.isSpeakerEnabled
        engine?.setSpeakerphoneEnabled(newState)
        updateCallState { if (this is CallUiState.InCall) copy(isSpeakerEnabled = newState) else this }
    }

    fun switchCamera() {
        engine?.switchCamera()
    }

    private companion object {
        const val JOIN_TIMEOUT_MS = 15_000L
    }
}
