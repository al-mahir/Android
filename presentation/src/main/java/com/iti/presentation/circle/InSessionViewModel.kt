package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.meeting.domain.repository.CircleRosterEvent
import com.iti.meeting.domain.repository.PendingJoinRequestEvent
import com.iti.meeting.presentation.circle.CircleAudioError
import com.iti.meeting.presentation.circle.CircleAudioSession
import com.iti.meeting.presentation.circle.CircleAudioSessionState
import com.iti.meeting.presentation.circle.CircleAudioStatus
import com.iti.presentation.R
import com.iti.presentation.circle.state.InSessionEffect
import com.iti.presentation.circle.state.InSessionIntent
import com.iti.presentation.circle.state.InSessionUiState
import com.iti.presentation.circle.state.SessionParticipant
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.domain.auth.MeetingCurrentUserProvider
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Owns a circle's live session: roster + host moderation over REST/STOMP, and the actual group
 * audio via [CircleAudioSession].
 *
 * The audio session deliberately lives in a Koin `single`, not in this ViewModel — leaving this
 * screen for the Mushaf must not drop the user out of the circle. This ViewModel only starts,
 * observes, and stops it.
 */
class InSessionViewModel(
    private val circleId: String,
    private val circleRepository: CircleRepository,
    private val currentUserProvider: MeetingCurrentUserProvider,
    private val audioSession: CircleAudioSession,
) : ViewModel(),
    StateHolder<InSessionUiState> by DefaultStateHolder(InSessionUiState()),
    EffectPublisher<InSessionEffect> by DefaultEffectPublisher() {

    init {
        observeCircle()
        observeRoster()
        observeAudioSession()
    }

    /**
     * Called by the screen on first composition. The mic permission gate runs before any join —
     * publishing audio without RECORD_AUDIO granted silently produces a session nobody can hear,
     * which was one of the reported symptoms.
     */
    fun onScreenReady() {
        sendEffect(InSessionEffect.RequestMicPermission)
    }

    fun onIntent(intent: InSessionIntent) = when (intent) {
        InSessionIntent.ToggleMic -> toggleMic()
        InSessionIntent.ToggleRaiseHand -> updateState { copy(isHandRaised = !isHandRaised) }
        InSessionIntent.Leave -> updateState { copy(isLeaveDialogVisible = true) }
        InSessionIntent.ConfirmLeave -> leaveCircle()
        InSessionIntent.DismissLeaveDialog -> updateState { copy(isLeaveDialogVisible = false) }
        InSessionIntent.OpenChat -> updateState { copy(unreadChatCount = 0) }
        InSessionIntent.OpenMushaf -> sendEffect(InSessionEffect.OpenMushaf)
        is InSessionIntent.ApproveRequest -> approve(intent.userId)
        is InSessionIntent.RejectRequest -> reject(intent.userId)
        is InSessionIntent.MicPermissionResult -> onMicPermissionResult(intent.granted)
        InSessionIntent.OpenAudioOutputPicker -> updateState { copy(isAudioOutputSheetVisible = true) }
        InSessionIntent.DismissAudioOutputPicker -> updateState { copy(isAudioOutputSheetVisible = false) }
        is InSessionIntent.SelectAudioDevice -> {
            audioSession.selectAudioDevice(intent.device)
            updateState { copy(isAudioOutputSheetVisible = false) }
        }
        InSessionIntent.RetryAudio -> joinAudio()
    }

    private fun onMicPermissionResult(granted: Boolean) {
        updateState { copy(isMicPermissionDenied = !granted) }
        // Join either way: a member who declined the mic can still listen, which is a perfectly
        // reasonable way to attend a study circle. They just can't unmute.
        joinAudio()
    }

    private fun joinAudio() {
        val circle = currentState.circle
        // The token endpoint only issues credentials while the circle is ONGOING, so don't even
        // try before then — surface the "not started" state directly instead of a failed fetch.
        if (circle != null && circle.status != CircleStatus.ONGOING) {
            updateState { copy(audioStatus = CircleAudioStatus.NotStarted) }
            return
        }
        audioSession.join(circleId = circleId, circleTitle = circle?.name.orEmpty())
    }

    private fun toggleMic() {
        if (currentState.isMicPermissionDenied) {
            sendEffect(InSessionEffect.RequestMicPermission)
            return
        }
        if (!currentState.isMicControlEnabled) return
        audioSession.toggleMic()
    }

    /** Folds live Agora state into the roster so speaking/mute indicators are real. */
    private fun observeAudioSession() {
        audioSession.state
            .onEach { session ->
                // Ignore state belonging to a different circle — the controller is a singleton, so
                // a stale emission from a previous session can arrive during a switch.
                if (session.circleId != null && session.circleId != circleId) return@onEach
                updateState {
                    copy(
                        audioStatus = session.status,
                        isMicMuted = !session.isMicEnabled,
                        audioDevice = session.audioDevice,
                        availableAudioDevices = session.availableAudioDevices,
                        participants = participants.mergeAudio(session),
                        speakingParticipantId = session.speakingUserAccount(),
                        isMicPermissionDenied = isMicPermissionDenied ||
                            (session.status as? CircleAudioStatus.Error)?.reason == CircleAudioError.MIC_PERMISSION_DENIED,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun leaveCircle() {
        if (currentState.isLeaving) return
        val isHost = currentState.isHost
        val status = currentState.circle?.status
        updateState { copy(isLeaving = true) }
        viewModelScope.launch {
            // Drop off the audio channel first: whichever way the REST call goes, staying
            // connected to a channel the user has chosen to leave is never right.
            audioSession.leave()

            val result = if (isHost) {
                if (status == CircleStatus.SCHEDULED) {
                    circleRepository.cancelCircle(circleId)
                } else {
                    circleRepository.endCircle(circleId)
                }
            } else {
                circleRepository.leaveCircle(circleId)
            }

            result.fold(
                onSuccess = {
                    updateState { copy(isLeaving = false, isLeaveDialogVisible = false) }
                    sendEffect(InSessionEffect.NavigateBack)
                },
                onFailure = {
                    updateState { copy(isLeaving = false, isLeaveDialogVisible = false) }
                    sendEffect(InSessionEffect.ShowMessage(R.string.circle_leave_error))
                },
            )
        }
    }

    private fun observeCircle() {
        viewModelScope.launch {
            val currentUserId = currentUserProvider.currentUserId()
            circleRepository.getCircle(circleId).fold(
                onSuccess = { circle ->
                    val isHost = circle.ownerId == currentUserId || circle.host?.userId == currentUserId
                    updateState { copy(circle = circle, isHost = isHost) }
                    // Start observing pending join requests only if this user is the host.
                    if (isHost) observePendingRequests()
                    // The circle details decide whether audio can start at all, so the first join
                    // attempt waits for them rather than racing the permission callback.
                    if (currentState.audioStatus is CircleAudioStatus.Idle) joinAudio()
                },
                onFailure = { /* keep last known circle; roster still renders */ },
            )
        }
    }

    private fun observeRoster() {
        viewModelScope.launch {
            circleRepository.getMembers(circleId).fold(
                onSuccess = { members ->
                    updateState {
                        copy(participants = members.map { it.toParticipant() }.mergeAudio(audioSession.currentState))
                    }
                },
                onFailure = { /* empty roster until events arrive */ },
            )
        }

        circleRepository.observeCircleEvents(circleId)
            .onEach { event ->
                when (event) {
                    is CircleRosterEvent.MemberJoined -> addParticipant(event.member)
                    is CircleRosterEvent.MemberLeft -> removeParticipant(event.userId)
                    is CircleRosterEvent.MemberRemoved -> removeParticipant(event.userId)
                    // The host started the circle while we were waiting — a token exists now.
                    CircleRosterEvent.Started -> onCircleStarted()
                    // Previously ignored, which left members sitting in a session that no longer
                    // existed, still holding an open Agora channel.
                    CircleRosterEvent.Ended,
                    CircleRosterEvent.Cancelled,
                    -> onCircleFinished()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onCircleStarted() {
        updateState { copy(circle = circle?.copy(status = CircleStatus.ONGOING)) }
        joinAudio()
    }

    private fun onCircleFinished() {
        audioSession.leave()
        sendEffect(InSessionEffect.ShowMessage(R.string.circle_session_ended))
        sendEffect(InSessionEffect.NavigateBack)
    }

    private fun addParticipant(member: CircleMember) {
        updateState {
            val exists = participants.any { it.id == member.userId }
            if (exists) this
            else copy(participants = (participants + member.toParticipant()).mergeAudio(audioSession.currentState))
        }
    }

    private fun removeParticipant(userId: String) {
        updateState { copy(participants = participants.filterNot { it.id == userId }) }
    }

    /** Streams live pending join requests for the host via STOMP. */
    private fun observePendingRequests() {
        viewModelScope.launch {
            // Seed with the current snapshot so requests already waiting are visible immediately.
            circleRepository.getPendingRequests(circleId).onSuccess { initial ->
                updateState { copy(pendingRequests = initial) }
            }
        }
        circleRepository.observePendingRequests(circleId)
            .onEach { event ->
                when (event) {
                    is PendingJoinRequestEvent.Received -> updateState {
                        copy(pendingRequests = (pendingRequests + event.request).distinctBy { it.membershipId })
                    }
                    is PendingJoinRequestEvent.Removed -> updateState {
                        copy(pendingRequests = pendingRequests.filterNot { it.membershipId == event.membershipId })
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun approve(userId: String) = runRequestAction(userId) {
        circleRepository.approveJoinRequest(circleId, userId)
    }

    private fun reject(userId: String) = runRequestAction(userId) {
        circleRepository.rejectJoinRequest(circleId, userId)
    }

    private fun runRequestAction(userId: String, block: suspend () -> Result<Unit>) {
        if (currentState.actionInProgress) return
        updateState { copy(actionInProgress = true) }
        viewModelScope.launch {
            block().fold(
                onSuccess = {
                    updateState {
                        copy(
                            actionInProgress = false,
                            pendingRequests = pendingRequests.filterNot { it.userId == userId },
                        )
                    }
                },
                onFailure = {
                    updateState { copy(actionInProgress = false) }
                    sendEffect(InSessionEffect.ShowMessage(R.string.circle_leave_error))
                },
            )
        }
    }
}

private fun CircleMember.toParticipant() = SessionParticipant(
    id = userId,
    name = displayName,
    initials = initials,
    isSpeaking = false,
    isMuted = true,
    isConnected = false,
)

/**
 * Overlays live Agora state onto the REST/STOMP roster.
 *
 * Matching is by roster `userId` against Agora's `userAccount`. Deliberately defensive: a member
 * with no matching Agora stream keeps their roster row and simply reads as not-yet-connected,
 * rather than vanishing — so if the backend's `userAccount` ever stops matching `userId`, the
 * roster degrades to static rather than emptying out. See the assumptions in
 * `docs/Circle-Audio-Fix-Plan.md`.
 */
private fun List<SessionParticipant>.mergeAudio(session: CircleAudioSessionState): List<SessionParticipant> {
    val byAccount = session.participantsByUserAccount
    return map { participant ->
        val audio = byAccount[participant.id]
        if (audio == null) {
            participant.copy(isSpeaking = false, isMuted = true, isConnected = false)
        } else {
            participant.copy(
                isSpeaking = audio.isSpeaking,
                isMuted = !audio.isMicEnabled,
                isConnected = true,
            )
        }
    }
}

/** Roster id of the current active speaker, when it can be resolved to one. */
private fun CircleAudioSessionState.speakingUserAccount(): String? =
    activeSpeakerUid?.let { remoteParticipants[it]?.userAccount }
