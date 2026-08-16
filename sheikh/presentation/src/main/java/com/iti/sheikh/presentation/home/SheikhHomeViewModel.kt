package com.iti.sheikh.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.connectivity.ConnectivityObserver
import com.iti.domain.connectivity.ConnectivityStatus
import com.iti.domain.core.getOrNull
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.Circle
import com.iti.sheikh.presentation.R
import com.iti.sheikh.presentation.core.mvi.DefaultEffectPublisher
import com.iti.sheikh.presentation.core.mvi.DefaultStateHolder
import com.iti.sheikh.presentation.core.mvi.EffectPublisher
import com.iti.sheikh.presentation.core.mvi.StateHolder
import com.iti.sheikh.presentation.home.state.SheikhHomeEffect
import com.iti.sheikh.presentation.home.state.SheikhHomeIntent
import com.iti.sheikh.presentation.home.state.SheikhHomeUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SheikhHomeViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val meetingRepository: MeetingRepository,
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<SheikhHomeUiState> by DefaultStateHolder(SheikhHomeUiState()),
    EffectPublisher<SheikhHomeEffect> by DefaultEffectPublisher() {

    private var contentJob: Job? = null

    init {
        observeContent()
        observeActiveCall()
    }

    fun onIntent(intent: SheikhHomeIntent) {
        when (intent) {
            SheikhHomeIntent.ProfileClicked -> sendEffect(SheikhHomeEffect.OpenProfile)
            SheikhHomeIntent.Retry -> observeContent()
            SheikhHomeIntent.RejoinActiveCallClicked -> rejoinActiveCall()
            SheikhHomeIntent.DismissActiveCallClicked -> dismissActiveCall()
        }
    }

    /** See the identical logic (and its rationale) in `:presentation`'s `HomeViewModel` — this is
     * the sheikh-side twin of the same case-3 rejoin flow from
     * docs/Meeting-Call-Lifecycle-Plan.md. */
    private fun observeActiveCall() {
        meetingRepository.observeActiveCall()
            .onEach { active -> updateState { copy(activeCall = active) } }
            .launchIn(viewModelScope)
    }

    private fun rejoinActiveCall() {
        val active = currentState.activeCall ?: return
        viewModelScope.launch {
            meetingRepository.refreshToken(active.requestId)
                .onSuccess { refreshed ->
                    sendEffect(
                        SheikhHomeEffect.OpenActiveCall(
                            requestId = active.requestId,
                            token = refreshed.token,
                            channelName = refreshed.channelName,
                            userAccount = refreshed.userAccount,
                            remoteDisplayName = active.remoteDisplayName,
                        ),
                    )
                }
                .onFailure {
                    meetingRepository.clearActiveCall()
                    sendEffect(SheikhHomeEffect.ShowMessage(R.string.sheikh_home_active_call_ended))
                }
        }
    }

    private fun dismissActiveCall() {
        val active = currentState.activeCall ?: return
        viewModelScope.launch {
            meetingRepository.endMeeting(active.requestId)
            meetingRepository.clearActiveCall()
        }
    }

    private fun observeContent() {
        contentJob?.cancel()
        updateState { copy(isLoading = true, errorMessageRes = null) }

        viewModelScope.launch {
            circleRepository.getMyCircles().fold(
                onSuccess = { circles -> updateState { copy(myCircles = circles) } },
                onFailure = { },
            )
        }

        viewModelScope.launch {
            circleRepository.getPublicCircles().fold(
                onSuccess = { circles ->
                    updateState {
                        copy(
                            availableCircles = circles.filter { circle ->
                                circle.status == CircleStatus.SCHEDULED ||
                                    circle.status == CircleStatus.ONGOING
                            },
                        )
                    }
                },
                onFailure = { },
            )
        }

        contentJob = combine(
            getCurrentUser(),
            connectivityObserver.status
        ) { userResult, connectionStatus ->
            val isOffline = connectionStatus == ConnectivityStatus.Unavailable
            val user = userResult.getOrNull()
            if (user == null) {
                updateState { copy(isLoading = false, errorMessageRes = R.string.sheikh_home_error_generic, isOffline = isOffline) }
                return@combine
            }
            updateState {
                copy(
                    isLoading = false,
                    errorMessageRes = null,
                    initials = user.initials,
                    avatarUrl = user.avatarUrl,
                    isOffline = isOffline
                )
            }
        }.catch { updateState { copy(isLoading = false, errorMessageRes = R.string.sheikh_home_error_generic) } }
        .launchIn(viewModelScope)
    }
}
