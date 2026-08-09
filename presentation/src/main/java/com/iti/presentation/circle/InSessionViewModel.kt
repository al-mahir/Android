package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.meeting.domain.repository.CircleRosterEvent
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

class InSessionViewModel(
    private val circleId: String,
    private val circleRepository: CircleRepository,
    private val currentUserProvider: MeetingCurrentUserProvider,
) : ViewModel(),
    StateHolder<InSessionUiState> by DefaultStateHolder(InSessionUiState()),
    EffectPublisher<InSessionEffect> by DefaultEffectPublisher() {

    init {
        observeCircle()
        observeRoster()
    }

    fun onIntent(intent: InSessionIntent) = when (intent) {
        InSessionIntent.ToggleMic -> updateState { copy(isMicMuted = !isMicMuted) }
        InSessionIntent.ToggleRaiseHand -> updateState { copy(isHandRaised = !isHandRaised) }
        InSessionIntent.Leave -> updateState { copy(isLeaveDialogVisible = true) }
        InSessionIntent.ConfirmLeave -> leaveCircle()
        InSessionIntent.DismissLeaveDialog -> updateState { copy(isLeaveDialogVisible = false) }
        InSessionIntent.OpenChat -> updateState { copy(unreadChatCount = 0) }
        InSessionIntent.OpenMushaf -> sendEffect(InSessionEffect.OpenMushaf)
    }

    private fun leaveCircle() {
        if (currentState.isLeaving) return
        val isHost = currentState.isHost
        val status = currentState.circle?.status
        updateState { copy(isLeaving = true) }
        viewModelScope.launch {
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
                        copy(
                            participants = members.map { it.toParticipant() },
                            speakingParticipantId = null,
                        )
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
                    CircleRosterEvent.Started,
                    CircleRosterEvent.Ended,
                    CircleRosterEvent.Cancelled,
                    -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    private fun addParticipant(member: CircleMember) {
        updateState {
            val exists = participants.any { it.id == member.userId }
            if (exists) this else copy(participants = participants + member.toParticipant())
        }
    }

    private fun removeParticipant(userId: String) {
        updateState { copy(participants = participants.filterNot { it.id == userId }) }
    }
}

private fun CircleMember.toParticipant() = SessionParticipant(
    id = userId,
    name = displayName,
    initials = initials,
    isSpeaking = false,
    isMuted = true,
)
