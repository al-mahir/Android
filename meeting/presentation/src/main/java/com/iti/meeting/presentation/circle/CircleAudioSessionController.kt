package com.iti.meeting.presentation.circle

import android.content.Context
import android.util.Log
import com.iti.meeting.domain.config.MeetingKitConfig
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.meeting.presentation.agora.AgoraEngineWrapper
import com.iti.meeting.presentation.call.audio.AudioOutputDevice
import com.iti.meeting.presentation.call.audio.AudioRouteController
import com.iti.meeting.presentation.call.session.OngoingCallActions
import com.iti.meeting.presentation.call.session.OngoingCallDisplay
import com.iti.meeting.presentation.call.session.OngoingCallRegistry
import com.iti.meeting.presentation.core.mvi.DefaultStateHolder
import com.iti.meeting.presentation.core.mvi.StateHolder
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "CircleAudio"

/**
 * Owns the Agora engine for a circle's group audio session.
 *
 * A sibling of `CallSessionController` rather than a reuse of it: the two share the low-level
 * plumbing ([AgoraEngineWrapper], [AudioRouteController], the call foreground service) but differ in
 * everything above it. A circle refreshes tokens against `/api/circles/{id}/token`, ends via the
 * circle resource, and has N remote participants shown as a roster — the 1:1 controller is wired to
 * the instant-meeting endpoints and models exactly one remote. See `docs/Circle-Audio-Fix-Plan.md`.
 *
 * A `single` in Koin, like its 1:1 sibling, so the session outlives the screen: navigating to the
 * Mushaf mid-circle must not drop the audio.
 */
class CircleAudioSessionController(
    private val appContext: Context,
    private val config: MeetingKitConfig,
    private val repository: CircleRepository,
    private val audioRoutes: AudioRouteController,
    private val ongoingCalls: OngoingCallRegistry,
) : CircleAudioSession,
    StateHolder<CircleAudioSessionState> by DefaultStateHolder(CircleAudioSessionState()) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var engine: AgoraEngineWrapper? = null
    private var durationJob: Job? = null
    private var joinTimeoutJob: Job? = null
    private var audioRoutesJob: Job? = null

    /** Kept so the notification can be re-rendered on a mute toggle without the caller re-supplying it. */
    private var circleTitle: String = ""

    /**
     * Joins [circleId]'s audio channel. Idempotent for the circle already joined, so a screen
     * recomposition or a return from the Mushaf doesn't tear down a live session and rebuild it.
     *
     * @param circleTitle shown on the ongoing-call notification.
     */
    override fun join(circleId: String, circleTitle: String) {
        val existing = currentState
        if (existing.circleId == circleId && existing.isLive) {
            Log.d(TAG, "join: already live for $circleId, skipping")
            return
        }
        if (existing.circleId != null && existing.circleId != circleId) {
            Log.w(TAG, "join: switching from circle ${existing.circleId} to $circleId, tearing down first")
            teardown()
        }
        if (engine != null || joinTimeoutJob != null) {
            Log.d(TAG, "join: a join is already in flight for $circleId, skipping")
            return
        }

        this.circleTitle = circleTitle
        updateState { CircleAudioSessionState(circleId = circleId, status = CircleAudioStatus.Connecting) }
        startAudioRouteTracking()
        ongoingCalls.start(
            context = appContext,
            display = display(circleTitle),
            actions = OngoingCallActions(onToggleMic = { toggleMic() }, onHangUp = { leave() }),
        )

        scope.launch {
            repository.getToken(circleId).fold(
                onSuccess = { token ->
                    Log.d(TAG, "getToken: SUCCESS circle=$circleId channel=${token.channelName} tokenLen=${token.token.length}")
                    startAgoraJoin(token.token, token.channelName, token.userAccount, circleTitle)
                },
                onFailure = { error ->
                    // The token endpoint is ONGOING-only, so a failure here is most often "the host
                    // hasn't started the circle yet" rather than a real fault. Surfacing that
                    // distinctly is the difference between an explainable screen and a dead one.
                    Log.w(TAG, "getToken failed for circle=$circleId", error)
                    ongoingCalls.stop(appContext)
                    updateState { copy(status = CircleAudioStatus.NotStarted) }
                },
            )
        }
    }

    private fun startAgoraJoin(
        token: String,
        channelName: String,
        userAccount: String,
        circleTitle: String,
    ) {
        if (engine != null) return
        Log.i(TAG, "startAgoraJoin: channel=$channelName userAccount=$userAccount appIdLen=${config.agoraAppId.length}")

        joinTimeoutJob = scope.launch {
            delay(JOIN_TIMEOUT_MS)
            if (currentState.status is CircleAudioStatus.Connecting) {
                Log.w(TAG, "join timed out after ${JOIN_TIMEOUT_MS}ms")
                updateState { copy(status = CircleAudioStatus.Error(CircleAudioError.CONNECT_FAILED)) }
                ongoingCalls.stop(appContext)
            }
        }

        val handler = object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, joinedUid: Int, elapsed: Int) {
                Log.i(TAG, "onJoinChannelSuccess: channel=$channel uid=$joinedUid elapsed=${elapsed}ms")
                joinTimeoutJob?.cancel()
                joinTimeoutJob = null
                updateState { copy(status = CircleAudioStatus.Live) }
                // Same ordering rule as the 1:1 call: the route only sticks once the channel is
                // joined, since COMMUNICATION profile re-decides it as part of joining.
                applyAudioRoute(audioRoutes.currentDevice)
                engine?.enableSpeakingIndication()
                startDurationTimer()
                ongoingCalls.update(display(circleTitle))
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                Log.i(TAG, "onUserJoined: uid=$uid")
                val joinedUid = uid
                updateState {
                    copy(
                        remoteParticipants = remoteParticipants + (joinedUid to CircleParticipantAudio(
                            uid = joinedUid,
                            userAccount = engine?.userAccountFor(joinedUid),
                        )),
                    )
                }
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                Log.i(TAG, "onUserOffline: uid=$uid reason=$reason")
                val leftUid = uid
                updateState {
                    copy(
                        remoteParticipants = remoteParticipants - leftUid,
                        activeSpeakerUid = activeSpeakerUid.takeIf { it != leftUid },
                    )
                }
            }

            /** The SDK's own uid→userAccount resolution, which can land after [onUserJoined]. */
            override fun onUserInfoUpdated(uid: Int, userInfo: UserInfo?) {
                val account = userInfo?.userAccount?.takeIf { it.isNotBlank() } ?: return
                Log.d(TAG, "onUserInfoUpdated: uid=$uid userAccount=$account")
                val updatedUid = uid
                updateState {
                    val existing = remoteParticipants[updatedUid] ?: CircleParticipantAudio(uid = updatedUid)
                    copy(remoteParticipants = remoteParticipants + (updatedUid to existing.copy(userAccount = account)))
                }
            }

            override fun onUserMuteAudio(uid: Int, muted: Boolean) {
                Log.d(TAG, "onUserMuteAudio: uid=$uid muted=$muted")
                val mutedUid = uid
                updateState {
                    val existing = remoteParticipants[mutedUid] ?: CircleParticipantAudio(uid = mutedUid)
                    copy(
                        remoteParticipants = remoteParticipants + (mutedUid to existing.copy(isMicEnabled = !muted)),
                    )
                }
            }

            /**
             * Loudness report for everyone audible right now. Agora reports the local user as
             * `uid == 0` in this callback, which is why the local flag is handled separately from
             * the remote map.
             */
            override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
                val volumes = speakers ?: return
                val speakingUids = volumes.filter { it.volume >= SPEAKING_VOLUME_THRESHOLD }.map { it.uid }.toSet()
                val localSpeaking = LOCAL_UID in speakingUids
                updateState {
                    copy(
                        isLocalSpeaking = localSpeaking && isMicEnabled,
                        remoteParticipants = remoteParticipants.mapValues { (uid, participant) ->
                            participant.copy(isSpeaking = uid in speakingUids)
                        },
                    )
                }
            }

            override fun onActiveSpeaker(uid: Int) {
                val speakerUid = uid
                updateState { copy(activeSpeakerUid = speakerUid.takeIf { it != LOCAL_UID }) }
            }

            override fun onConnectionStateChanged(state: Int, reason: Int) {
                Log.i(TAG, "onConnectionStateChanged: state=$state reason=$reason")
                if (state == Constants.CONNECTION_STATE_FAILED) {
                    joinTimeoutJob?.cancel()
                    joinTimeoutJob = null
                    updateState { copy(status = CircleAudioStatus.Error(CircleAudioError.CONNECTION_LOST)) }
                    ongoingCalls.stop(appContext)
                }
            }

            override fun onAudioRouteChanged(routing: Int) {
                AudioOutputDevice.fromAgoraRoute(routing)?.let(audioRoutes::onEngineReportedRoute)
            }

            override fun onTokenPrivilegeWillExpire(token: String?) {
                Log.w(TAG, "onTokenPrivilegeWillExpire")
                renewToken()
            }

            override fun onRequestToken() {
                Log.w(TAG, "onRequestToken: current token was rejected/expired")
                renewToken()
            }

            override fun onError(err: Int) {
                Log.e(TAG, "onError: code=$err")
                // Don't tear a live session down over a transient SDK error — only a failed join,
                // where there is nothing to keep, becomes a visible error state.
                if (currentState.status is CircleAudioStatus.Connecting) {
                    joinTimeoutJob?.cancel()
                    joinTimeoutJob = null
                    updateState { copy(status = CircleAudioStatus.Error(CircleAudioError.CONNECT_FAILED)) }
                    ongoingCalls.stop(appContext)
                }
            }
        }

        val wrapper = AgoraEngineWrapper(appContext, config.agoraAppId, handler, audioOnly = true)
        engine = wrapper
        // Circles join muted: arriving in a study circle with a hot mic is the kind of surprise
        // that makes people stop using the feature. The user opts in with the mic control.
        wrapper.joinChannel(token, channelName, userAccount, publishAudio = false, publishVideo = false)
    }

    override fun toggleMic(): Boolean {
        val enabled = !currentState.isMicEnabled
        setMicEnabled(enabled)
        return enabled
    }

    fun setMicEnabled(enabled: Boolean) {
        if (currentState.status !is CircleAudioStatus.Live) return
        engine?.setLocalAudioEnabled(enabled)
        updateState { copy(isMicEnabled = enabled, isLocalSpeaking = isLocalSpeaking && enabled) }
        ongoingCalls.update(display(circleTitle))
    }

    override fun selectAudioDevice(device: AudioOutputDevice) = audioRoutes.select(device)

    /** Leaves the Agora channel. The REST leave/end call is the caller's job — the circle's
     * membership lifecycle is a different concern from its audio transport. */
    override fun leave() {
        Log.d(TAG, "leave: circle=${currentState.circleId}")
        teardown()
    }

    private fun teardown() {
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
        ongoingCalls.stop(appContext)
        updateState { CircleAudioSessionState() }
    }

    private fun startAudioRouteTracking() {
        audioRoutesJob?.cancel()
        audioRoutes.start()
        audioRoutesJob = audioRoutes.state
            .onEach { routeState ->
                engine?.setAudioRoute(routeState.selected.agoraRoute)
                updateState {
                    copy(audioDevice = routeState.selected, availableAudioDevices = routeState.available)
                }
            }
            .launchIn(scope)
    }

    /** Mirrors the 1:1 controller's re-assert: some OEM audio HALs reset the route a beat after
     * the channel is joined, which is what made the earpiece bug intermittent there. */
    private fun applyAudioRoute(device: AudioOutputDevice) {
        engine?.setAudioRoute(device.agoraRoute)
        scope.launch {
            delay(ROUTE_REASSERT_DELAY_MS)
            if (currentState.status is CircleAudioStatus.Live) {
                engine?.setAudioRoute(audioRoutes.currentDevice.agoraRoute)
            }
        }
    }

    private fun renewToken() {
        val circleId = currentState.circleId ?: return
        scope.launch {
            repository.getToken(circleId)
                .onSuccess { engine?.renewToken(it.token) }
                .onFailure { Log.e(TAG, "renewToken: getToken($circleId) failed — the session will drop when the current token expires", it) }
        }
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (true) {
                delay(1_000)
                updateState { copy(sessionDurationSeconds = sessionDurationSeconds + 1) }
            }
        }
    }

    private fun display(circleTitle: String) = OngoingCallDisplay(
        title = circleTitle,
        isMicEnabled = currentState.isMicEnabled,
        isConnecting = currentState.status is CircleAudioStatus.Connecting,
    )

    private companion object {
        const val JOIN_TIMEOUT_MS = 15_000L
        const val ROUTE_REASSERT_DELAY_MS = 1_200L

        /** Agora reports the local user under uid 0 in volume callbacks. */
        const val LOCAL_UID = 0

        /** Out of 255. Low enough to catch quiet speech, high enough to ignore room noise. */
        const val SPEAKING_VOLUME_THRESHOLD = 15
    }
}
