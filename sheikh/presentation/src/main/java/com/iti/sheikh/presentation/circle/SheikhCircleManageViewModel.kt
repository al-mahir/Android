package com.iti.sheikh.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.meeting.domain.repository.CircleRosterEvent
import com.iti.meeting.domain.repository.PendingJoinRequestEvent
import com.iti.sheikh.presentation.R
import com.iti.sheikh.presentation.circle.state.SheikhCircleManageEffect
import com.iti.sheikh.presentation.circle.state.SheikhCircleManageIntent
import com.iti.sheikh.presentation.circle.state.SheikhCircleManageUiState
import com.iti.sheikh.presentation.core.mvi.DefaultEffectPublisher
import com.iti.sheikh.presentation.core.mvi.DefaultStateHolder
import com.iti.sheikh.presentation.core.mvi.EffectPublisher
import com.iti.sheikh.presentation.core.mvi.StateHolder
import kotlinx.coroutines.launch

class SheikhCircleManageViewModel(
    private val circleId: String,
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<SheikhCircleManageUiState> by DefaultStateHolder(SheikhCircleManageUiState()),
    EffectPublisher<SheikhCircleManageEffect> by DefaultEffectPublisher() {

    init {
        load()
        observeLiveUpdates()
    }

    fun onIntent(intent: SheikhCircleManageIntent) = when (intent) {
        SheikhCircleManageIntent.Retry -> load()
        is SheikhCircleManageIntent.ApproveRequest -> approve(intent.userId)
        is SheikhCircleManageIntent.RejectRequest -> reject(intent.userId)
        is SheikhCircleManageIntent.RemoveMember -> removeMember(intent.userId)
        SheikhCircleManageIntent.StartClicked -> start()
        SheikhCircleManageIntent.EndClicked -> end()
        SheikhCircleManageIntent.CancelClicked -> cancel()
    }

    private fun load() {
        updateState { copy(isLoading = true, isError = false) }
        viewModelScope.launch {
            val circle = circleRepository.getCircle(circleId)
            val pending = circleRepository.getPendingRequests(circleId)
            val members = circleRepository.getMembers(circleId)
            updateState {
                copy(
                    circle = circle.getOrNull(),
                    pendingRequests = pending.getOrNull().orEmpty(),
                    members = members.getOrNull().orEmpty(),
                    isLoading = false,
                    isError = circle.isFailure,
                )
            }
        }
    }

    private fun observeLiveUpdates() {
        viewModelScope.launch {
            circleRepository.observePendingRequests(circleId).collect { event ->
                when (event) {
                    is PendingJoinRequestEvent.Received -> updateState {
                        copy(pendingRequests = (pendingRequests + event.request).distinctBy { it.membershipId })
                    }
                    is PendingJoinRequestEvent.Removed -> updateState {
                        copy(pendingRequests = pendingRequests.filterNot { it.membershipId == event.membershipId })
                    }
                }
            }
        }
        viewModelScope.launch {
            circleRepository.observeCircleEvents(circleId).collect { event ->
                when (event) {
                    is CircleRosterEvent.MemberJoined,
                    is CircleRosterEvent.MemberLeft,
                    is CircleRosterEvent.MemberRemoved,
                    -> refreshMembers()
                    is CircleRosterEvent.Started,
                    is CircleRosterEvent.Ended,
                    is CircleRosterEvent.Cancelled,
                    -> refreshCircle()
                }
            }
        }
    }

    private fun refreshMembers() {
        viewModelScope.launch {
            circleRepository.getMembers(circleId).onSuccess { members ->
                updateState { copy(members = members) }
            }
        }
    }

    private fun refreshCircle() {
        viewModelScope.launch {
            circleRepository.getCircle(circleId).onSuccess { circle ->
                updateState { copy(circle = circle) }
            }
        }
    }

    private fun approve(userId: String) = runMemberAction(userId) { id ->
        circleRepository.approveJoinRequest(circleId, id)
    }

    private fun reject(userId: String) = runMemberAction(userId) { id ->
        circleRepository.rejectJoinRequest(circleId, id)
    }

    private fun removeMember(userId: String) = runMemberAction(userId) { id ->
        circleRepository.removeMember(circleId, id)
    }

    private fun runMemberAction(
        userId: String,
        block: suspend (String) -> Result<Unit>,
    ) {
        if (currentState.actionInProgress) return
        updateState { copy(actionInProgress = true) }
        viewModelScope.launch {
            block(userId).fold(
                onSuccess = {
                    updateState {
                        copy(
                            actionInProgress = false,
                            pendingRequests = pendingRequests.filterNot { it.userId == userId },
                        )
                    }
                    refreshMembers()
                },
                onFailure = {
                    updateState { copy(actionInProgress = false) }
                    sendEffect(SheikhCircleManageEffect.ShowMessage(R.string.sheikh_circle_action_failed))
                },
            )
        }
    }

    private fun start() {
        if (currentState.actionInProgress) return
        updateState { copy(actionInProgress = true) }
        viewModelScope.launch {
            circleRepository.startCircle(circleId).fold(
                onSuccess = {
                    updateState { copy(actionInProgress = false, circle = it) }
                    sendEffect(SheikhCircleManageEffect.ShowMessage(R.string.sheikh_circle_started))
                },
                onFailure = {
                    updateState { copy(actionInProgress = false) }
                    sendEffect(SheikhCircleManageEffect.ShowMessage(R.string.sheikh_circle_action_failed))
                },
            )
        }
    }

    private fun end() {
        if (currentState.actionInProgress) return
        updateState { copy(actionInProgress = true) }
        viewModelScope.launch {
            circleRepository.endCircle(circleId).fold(
                onSuccess = {
                    updateState { copy(actionInProgress = false) }
                    refreshCircle()
                    sendEffect(SheikhCircleManageEffect.ShowMessage(R.string.sheikh_circle_ended))
                },
                onFailure = {
                    updateState { copy(actionInProgress = false) }
                    sendEffect(SheikhCircleManageEffect.ShowMessage(R.string.sheikh_circle_action_failed))
                },
            )
        }
    }

    private fun cancel() {
        if (currentState.actionInProgress) return
        updateState { copy(actionInProgress = true) }
        viewModelScope.launch {
            circleRepository.cancelCircle(circleId).fold(
                onSuccess = {
                    updateState { copy(actionInProgress = false) }
                    refreshCircle()
                    sendEffect(SheikhCircleManageEffect.ShowMessage(R.string.sheikh_circle_cancelled))
                },
                onFailure = {
                    updateState { copy(actionInProgress = false) }
                    sendEffect(SheikhCircleManageEffect.ShowMessage(R.string.sheikh_circle_action_failed))
                },
            )
        }
    }
}
