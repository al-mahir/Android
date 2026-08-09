package com.iti.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.fold
import com.iti.domain.core.getOrNull
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.domain.usecase.reading.GetAyahOfTheDayUseCase
import com.iti.domain.usecase.reading.GetReadingProgressUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.home.state.HomeEffect
import com.iti.presentation.home.state.HomeIntent
import com.iti.presentation.home.state.HomeUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch


import com.iti.domain.connectivity.ConnectivityObserver
import com.iti.domain.connectivity.ConnectivityStatus

class HomeViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getReadingProgress: GetReadingProgressUseCase,
    private val getAyahOfTheDay: GetAyahOfTheDayUseCase,
    private val getSheikhs: GetSheikhsUseCase,
    private val getStudyCircles: GetStudyCirclesUseCase,
    private val joinStudyCircle: JoinStudyCircleUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val meetingRepository: MeetingRepository,
) : ViewModel(),
    StateHolder<HomeUiState> by DefaultStateHolder(HomeUiState()),
    EffectPublisher<HomeEffect> by DefaultEffectPublisher() {

    private var contentJob: Job? = null

    init {
        observeContent()
        observePendingMeetingRequest()
        observeActiveCall()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Retry -> observeContent()
            HomeIntent.SearchClicked -> sendEffect(HomeEffect.OpenSearch)
            HomeIntent.ProfileClicked -> sendEffect(HomeEffect.OpenProfile)
            HomeIntent.SeeAllSheikhsClicked -> sendEffect(HomeEffect.OpenSheikhList)
            HomeIntent.SeeAllCirclesClicked -> sendEffect(HomeEffect.OpenCircleList)
            HomeIntent.ContinueReadingClicked -> openReadingProgress()
            is HomeIntent.SheikhClicked -> sendEffect(HomeEffect.OpenSheikh(intent.sheikhId))
            is HomeIntent.JoinCircleClicked -> join(intent.circleId)
            HomeIntent.ViewPendingMeetingClicked -> viewPendingMeeting()
            HomeIntent.CancelPendingMeetingClicked -> cancelPendingMeeting()
            HomeIntent.RejoinActiveCallClicked -> rejoinActiveCall()
            HomeIntent.DismissActiveCallClicked -> dismissActiveCall()
            HomeIntent.StartExamClicked -> sendEffect(HomeEffect.OpenExamSetup)
        }
    }

    private fun observePendingMeetingRequest() {
        meetingRepository.observePendingRequest()
            .onEach { pending -> updateState { copy(pendingMeetingRequest = pending) } }
            .launchIn(viewModelScope)
    }

    private fun viewPendingMeeting() {
        val pending = currentState.pendingMeetingRequest ?: return
        sendEffect(HomeEffect.OpenMeetingRequest(pending.sheikhId, pending.sheikhName))
    }

    private fun cancelPendingMeeting() {
        val pending = currentState.pendingMeetingRequest ?: return
        viewModelScope.launch {
            meetingRepository.cancelMeetingRequest(pending.requestId)
        }
    }

    private fun observeActiveCall() {
        meetingRepository.observeActiveCall()
            .onEach { active -> updateState { copy(activeCall = active) } }
            .launchIn(viewModelScope)
    }

    /** Never auto-rejoins silently — [MeetingRepository.refreshToken] doubles as a liveness probe
     * here: a fresh token means the call is still active server-side, a failure means it ended
     * while this app process was dead (or never actually started this session at all). See case 3
     * in docs/Meeting-Call-Lifecycle-Plan.md. */
    private fun rejoinActiveCall() {
        val active = currentState.activeCall ?: return
        viewModelScope.launch {
            meetingRepository.refreshToken(active.requestId)
                .onSuccess { refreshed ->
                    sendEffect(
                        HomeEffect.OpenActiveCall(
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
                    sendEffect(HomeEffect.ShowMessage(R.string.home_active_call_ended))
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
        // Cancel any in-flight collection so a retry cannot leave two streams writing state.
        contentJob?.cancel()
        updateState { copy(isLoading = true, errorMessageRes = null) }

        // Load sheikhs from real API in parallel (one-shot suspend).
        viewModelScope.launch {
            getSheikhs().fold(
                onSuccess = { sheikhs -> updateState { copy(sheikhs = sheikhs) } },
                onError = { /* Home shows error only if all sources fail; ignore partial sheikh failure */ },
            )
        }

        // Observe user, reading progress, ayah of the day, circles, and connectivity.
        contentJob = combine(
            getCurrentUser(),
            getReadingProgress(),
            getAyahOfTheDay(),
            getStudyCircles(),
            connectivityObserver.status
        ) { userResult, readingProgress, ayahOfTheDay, circlesResult, connectivity ->
            val user = userResult.getOrNull()
            val circles = circlesResult.getOrNull()
            if (user == null || circles == null) {
                null
            } else {
                val isOffline = connectivity == ConnectivityStatus.Unavailable
                HomeContentSnapshot(user, readingProgress, ayahOfTheDay, emptyList(), circles, isOffline)
            }
        }
            .onEach { snapshot ->
                if (snapshot == null) {
                    updateState { copy(isLoading = false, errorMessageRes = R.string.home_error_generic) }
                } else {
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessageRes = null,
                            user = snapshot.user,
                            readingProgress = snapshot.readingProgress,
                            ayahOfTheDay = snapshot.ayahOfTheDay,
                            circles = snapshot.circles.take(2),
                            isOffline = snapshot.isOffline,
                        )
                    }
                }
            }
            .catch { updateState { copy(isLoading = false, errorMessageRes = R.string.home_error_generic) } }
            .launchIn(viewModelScope)
    }

    private fun openReadingProgress() {
        val page = currentState.readingProgress?.pageNumber ?: return
        sendEffect(HomeEffect.OpenMushafAtPage(page))
    }

    private fun join(circleId: String) {
        if (circleId in currentState.joiningCircleIds) return
        updateState { copy(joiningCircleIds = joiningCircleIds + circleId) }

        viewModelScope.launch {
            runCatching { joinStudyCircle(circleId) }
                .onFailure { sendEffect(HomeEffect.ShowMessage(R.string.home_join_failed)) }
            updateState { copy(joiningCircleIds = joiningCircleIds - circleId) }
        }
    }
}
