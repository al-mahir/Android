package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.model.circle.CircleMember
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class InSessionViewModel(
    private val circleId: String,
    private val circleRepository: CircleRepository,
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
        updateState { copy(isLeaving = true) }
        viewModelScope.launch {
            circleRepository.leaveCircle(circleId).fold(
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
            circleRepository.getCircle(circleId).fold(
                onSuccess = { circle -> updateState { copy(circle = circle) } },
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
