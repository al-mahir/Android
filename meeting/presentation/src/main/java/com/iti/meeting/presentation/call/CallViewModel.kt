package com.iti.meeting.presentation.call

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.presentation.agora.AgoraEngineWrapper
import com.iti.meeting.domain.config.MeetingKitConfig
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.meeting.domain.repository.MeetingRequestEvent
import com.iti.meeting.presentation.core.mvi.DefaultStateHolder
import com.iti.meeting.presentation.core.mvi.StateHolder
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "MeetingCall"

class CallViewModel(
    private val config: MeetingKitConfig,
    private val repository: MeetingRepository,
) : ViewModel(), StateHolder<CallUiState> by DefaultStateHolder(CallUiState.Connecting) {

    var engine: AgoraEngineWrapper? = null
        private set

    private var durationJob: Job? = null
    private var eventsJob: Job? = null
    private var requestId: String? = null

    private var joinTimeoutJob: Job? = null

       fun prepareForRequest(newRequestId: String) {
        Log.d(TAG, "prepareForRequest: newRequestId=$newRequestId, this.requestId=$requestId, currentState=$currentState")
        if (requestId != null && requestId != newRequestId) {
            Log.w(TAG, "prepareForRequest: stale state from previous call $requestId, resetting for $newRequestId")
            teardownForRejoin()
            updateState { CallUiState.Connecting }
        }
    }

    fun joinChannel(
        context: Context,
        requestId: String,
        token: String,
        channelName: String,
        userAccount: String,
        micEnabled: Boolean,
        cameraEnabled: Boolean,
    ) {
        Log.d(TAG, "joinChannel: called requestId=$requestId, this.requestId=${this.requestId}, engine=$engine, joinTimeoutJob=$joinTimeoutJob")
        if (this.requestId != null && this.requestId != requestId) {
                      Log.w(TAG, "joinChannel: stale state from previous call ${this.requestId}, tearing down before joining $requestId")
            teardownForRejoin()
        }
        if (engine != null || joinTimeoutJob != null) {
            Log.d(TAG, "joinChannel: already joined/joining requestId=$requestId, skipping")
            return
        }
        this.requestId = requestId
        updateState { CallUiState.Connecting }
        observeMeetingEnded(requestId)

              viewModelScope.launch {
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

       private fun teardownForRejoin() {
        durationJob?.cancel()
        durationJob = null
        eventsJob?.cancel()
        eventsJob = null
        joinTimeoutJob?.cancel()
        joinTimeoutJob = null
        engine?.leaveChannel()
        engine?.destroy()
        engine = null
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

        joinTimeoutJob = viewModelScope.launch {
            delay(JOIN_TIMEOUT_MS)
            if (currentState is CallUiState.Connecting) {
                updateState { CallUiState.Error("Couldn't connect to the call. Please try again.") }
            }
        }

        val eventHandler = object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, joinedUid: Int, elapsed: Int) {
                Log.i(TAG, "onJoinChannelSuccess: channel=$channel uid=$joinedUid elapsed=${elapsed}ms")
                joinTimeoutJob?.cancel()
                joinTimeoutJob = null
                updateState {
                    if (this is CallUiState.InCall) this
                    else CallUiState.InCall(remoteUid = null, isMicEnabled = micEnabled, isCameraEnabled = cameraEnabled)
                }
                startDurationTimer()
            }

            override fun onUserJoined(remoteUid: Int, elapsed: Int) {
                Log.i(TAG, "onUserJoined: remoteUid=$remoteUid elapsed=${elapsed}ms")
                updateState {
                    if (this is CallUiState.InCall) copy(remoteUid = remoteUid)
                    else CallUiState.InCall(remoteUid = remoteUid, isMicEnabled = micEnabled, isCameraEnabled = cameraEnabled)
                }
            }

            override fun onUserOffline(remoteUid: Int, reason: Int) {
                Log.i(TAG, "onUserOffline: remoteUid=$remoteUid reason=$reason")
                updateState {
                    if (this is CallUiState.InCall) copy(remoteUid = null, isRemoteMicEnabled = true, isRemoteCameraEnabled = true)
                    else this
                }
            }

            override fun onUserMuteAudio(remoteUid: Int, muted: Boolean) {
                Log.i(TAG, "onUserMuteAudio: remoteUid=$remoteUid muted=$muted")
                updateState {
                    if (this is CallUiState.InCall && remoteUid == this.remoteUid) copy(isRemoteMicEnabled = !muted) else this
                }
            }

            override fun onUserMuteVideo(remoteUid: Int, muted: Boolean) {
                Log.i(TAG, "onUserMuteVideo: remoteUid=$remoteUid muted=$muted")
                updateState {
                    if (this is CallUiState.InCall && remoteUid == this.remoteUid) copy(isRemoteCameraEnabled = !muted) else this
                }
            }

            override fun onConnectionStateChanged(state: Int, reason: Int) {
                Log.i(TAG, "onConnectionStateChanged: state=$state reason=$reason")
                when (state) {
                    Constants.CONNECTION_STATE_RECONNECTING ->
                        updateState { if (this is CallUiState.InCall) copy(isReconnecting = true) else this }
                    Constants.CONNECTION_STATE_CONNECTED ->
                        updateState { if (this is CallUiState.InCall) copy(isReconnecting = false) else this }
                    Constants.CONNECTION_STATE_FAILED -> {
                        joinTimeoutJob?.cancel()
                        joinTimeoutJob = null
                        updateState { CallUiState.Error("Connection lost. Please try again.") }
                    }
                }
            }

            override fun onError(err: Int) {
                Log.e(TAG, "onError: code=$err")
                joinTimeoutJob?.cancel()
                joinTimeoutJob = null
                updateState { CallUiState.Error("Call error ($err)") }
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
                    updateState { CallUiState.Ended }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun renewToken() {
        val id = requestId ?: return
        viewModelScope.launch {
            repository.refreshToken(id).onSuccess { refreshed ->
                engine?.renewToken(refreshed.token)
            }
        }
    }

    /** Called when the local user explicitly leaves — tells the backend so it can reset the sheikh to AVAILABLE. */
    fun endCall() {
        val id = requestId
        if (id != null) viewModelScope.launch { repository.endMeeting(id) }
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                updateState { if (this is CallUiState.InCall) copy(callDurationSeconds = callDurationSeconds + 1) else this }
            }
        }
    }

    fun toggleMic() {
        val current = currentState as? CallUiState.InCall ?: return
        val newState = !current.isMicEnabled
        engine?.setLocalAudioEnabled(newState)
        updateState {
            if (this is CallUiState.InCall) copy(isMicEnabled = newState) else this
        }
    }

    fun toggleCamera() {
        val current = currentState as? CallUiState.InCall ?: return
        val newState = !current.isCameraEnabled
        engine?.setLocalVideoEnabled(newState)
        updateState {
            if (this is CallUiState.InCall) copy(isCameraEnabled = newState) else this
        }
    }

    fun toggleSpeaker() {
        val current = currentState as? CallUiState.InCall ?: return
        val newState = !current.isSpeakerEnabled
        engine?.setSpeakerphoneEnabled(newState)
        updateState {
            if (this is CallUiState.InCall) copy(isSpeakerEnabled = newState) else this
        }
    }

    fun switchCamera() {
        engine?.switchCamera()
    }

    override fun onCleared() {
        super.onCleared()
        durationJob?.cancel()
        eventsJob?.cancel()
        joinTimeoutJob?.cancel()
        engine?.leaveChannel()
        engine?.destroy()
        engine = null
    }

    private companion object {
        const val JOIN_TIMEOUT_MS = 15_000L
    }
}
