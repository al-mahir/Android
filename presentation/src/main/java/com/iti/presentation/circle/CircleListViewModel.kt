package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleJoinError
import com.iti.meeting.domain.model.circle.CircleJoinResult
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.presentation.R
import com.iti.presentation.circle.state.CircleListEffect
import com.iti.presentation.circle.state.CircleListIntent
import com.iti.presentation.circle.state.CircleListUiState
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class CircleListViewModel(
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<CircleListUiState> by DefaultStateHolder(CircleListUiState()),
    EffectPublisher<CircleListEffect> by DefaultEffectPublisher() {


    init {
        load()
        loadMyCircles()
    }

    fun onIntent(intent: CircleListIntent) = when (intent) {
        is CircleListIntent.SearchQueryChanged -> updateSearch(intent.query)
        is CircleListIntent.StatusSelected -> updateStatus(intent.status)
        is CircleListIntent.CircleClicked -> sendEffect(CircleListEffect.OpenCircle(intent.circleId))
        CircleListIntent.CreateCircleClicked -> sendEffect(CircleListEffect.OpenCreateCircle)
        CircleListIntent.JoinPrivateClicked -> updateState {
            copy(joinSheetVisible = true, joinErrorRes = null)
        }
        is CircleListIntent.JoinCircleIdChanged -> updateState {
            copy(joinCircleId = intent.circleId, joinErrorRes = null)
        }
        is CircleListIntent.JoinPasswordChanged -> updateState { copy(joinPassword = intent.password) }
        is CircleListIntent.JoinTokenChanged -> updateState { copy(joinToken = intent.token, joinErrorRes = null) }
        is CircleListIntent.JoinModeChanged -> updateState { copy(joinByToken = intent.byToken, joinErrorRes = null) }
        CircleListIntent.SubmitJoinPrivate -> submitJoin()
        CircleListIntent.SubmitJoinViaToken -> submitJoinViaToken()
        CircleListIntent.DismissJoinPrivate -> updateState { closeJoinSheet() }
        CircleListIntent.Retry -> load()
        CircleListIntent.Refresh -> loadMyCircles()
        CircleListIntent.PullToRefresh -> pullToRefresh()
    }

    private fun load() {
        updateState { copy(isLoading = true, isError = false) }
        viewModelScope.launch { fetchPublicCircles(keepContentOnFailure = false) }
    }

    /**
     * Swipe-to-refresh. Unlike [load] it never raises [CircleListUiState.isLoading], so the list
     * the user is reading stays put and only the pull indicator spins. Re-entrant pulls are
     * dropped: the gesture can fire again before `isRefreshing` has reached the UI.
     */
    private fun pullToRefresh() {
        if (currentState.isRefreshing) return
        updateState { copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                joinAll(
                    launch { fetchPublicCircles(keepContentOnFailure = true) },
                    launch { fetchMyCircles() },
                )
            } finally {
                // Also runs if the ViewModel is cleared mid-refresh, so the flag never sticks.
                updateState { copy(isRefreshing = false) }
            }
        }
    }

    /**
     * @param keepContentOnFailure true for a refresh over an already-populated list: a transient
     *   failure reports itself as a message instead of throwing away circles the user can still
     *   read. An empty list has nothing to protect, so it falls through to the error screen.
     */
    private suspend fun fetchPublicCircles(keepContentOnFailure: Boolean) {
        circleRepository.getPublicCircles().fold(
            onSuccess = { circles ->
                updateState {
                    copy(
                        circles = circles,
                        filteredCircles = buildFilteredList(circles, myCircles, searchQuery, selectedStatus),
                        isLoading = false,
                        isError = false,
                    )
                }
            },
            onFailure = {
                val hasContent = currentState.filteredCircles.isNotEmpty()
                if (keepContentOnFailure && hasContent) {
                    updateState { copy(isLoading = false) }
                    sendEffect(CircleListEffect.ShowMessage(R.string.refresh_failed))
                } else {
                    updateState { copy(isLoading = false, isError = true) }
                }
            },
        )
    }

    /** Joined circles (including PRIVATE ones) are merged into the list so the user keeps seeing
     * them after joining, and marked so the card can show the joined state. */
    private fun loadMyCircles() {
        viewModelScope.launch { fetchMyCircles() }
    }

    private suspend fun fetchMyCircles() {
        circleRepository.getMyCircles().fold(
            onSuccess = { mine ->
                updateState {
                    copy(
                        myCircles = mine,
                        joinedCircleIds = mine.mapTo(mutableSetOf()) { it.id },
                        filteredCircles = buildFilteredList(circles, mine, searchQuery, selectedStatus),
                    )
                }
            },
            onFailure = { /* Joined markers are optional; a failure leaves the list unmarked. */ },
        )
    }

    private fun updateSearch(query: String) {
        updateState {
            copy(
                searchQuery = query,
                filteredCircles = buildFilteredList(circles, myCircles, query, selectedStatus),
            )
        }
    }

    private fun updateStatus(status: CircleStatus?) {
        updateState {
            copy(
                selectedStatus = status,
                filteredCircles = buildFilteredList(circles, myCircles, searchQuery, status),
            )
        }
    }

    private fun submitJoin() {
        if (currentState.isJoining) return
        val circleId = currentState.joinCircleId.trim()
        if (circleId.isBlank()) {
            updateState { copy(joinErrorRes = R.string.circle_join_id_required) }
            return
        }

        updateState { copy(isJoining = true, joinErrorRes = null) }
        viewModelScope.launch {
            when (val result = circleRepository.joinCircle(circleId, currentState.joinPassword)) {
                is CircleJoinResult.Joined -> {
                    updateState { closeJoinSheet() }
                    sendEffect(CircleListEffect.ShowMessage(R.string.circle_join_success))
                    loadMyCircles()
                }

                is CircleJoinResult.PendingApproval -> {
                    updateState { closeJoinSheet() }
                    sendEffect(CircleListEffect.ShowMessage(R.string.circle_join_request_sent))
                }

                is CircleJoinResult.Error -> {
                    if (result.error == CircleJoinError.ALREADY_MEMBER) {
                        updateState { closeJoinSheet() }
                        loadMyCircles()
                        sendEffect(CircleListEffect.ShowMessage(R.string.circle_join_error_already_member))
                    } else {
                        updateState { copy(isJoining = false, joinErrorRes = result.error.messageRes()) }
                    }
                }
            }
        }
    }

    private fun submitJoinViaToken() {
        if (currentState.isJoining) return
        val token = currentState.joinToken.trim()
        if (token.isBlank()) {
            updateState { copy(joinErrorRes = R.string.circle_join_token_required) }
            return
        }

        updateState { copy(isJoining = true, joinErrorRes = null) }
        viewModelScope.launch {
            when (val result = circleRepository.joinCircleViaToken(token)) {
                is CircleJoinResult.Joined -> {
                    updateState { closeJoinSheet() }
                    sendEffect(CircleListEffect.ShowMessage(R.string.circle_join_success))
                    loadMyCircles()
                }

                is CircleJoinResult.PendingApproval -> {
                    updateState { closeJoinSheet() }
                    sendEffect(CircleListEffect.ShowMessage(R.string.circle_join_request_sent))
                }

                is CircleJoinResult.Error -> {
                    if (result.error == CircleJoinError.ALREADY_MEMBER) {
                        updateState { closeJoinSheet() }
                        loadMyCircles()
                        sendEffect(CircleListEffect.ShowMessage(R.string.circle_join_error_already_member))
                    } else {
                        updateState { copy(isJoining = false, joinErrorRes = result.error.messageRes()) }
                    }
                }
            }
        }
    }

    private fun CircleJoinError.messageRes(): Int = when (this) {
        CircleJoinError.CIRCLE_FULL -> R.string.circle_join_error_full
        CircleJoinError.TIME_CONFLICT -> R.string.circle_join_error_conflict
        CircleJoinError.INVALID_PASSWORD -> R.string.circle_join_error_password
        CircleJoinError.ALREADY_MEMBER -> R.string.circle_join_error_already_member
        CircleJoinError.UNKNOWN -> R.string.circle_join_error_generic
    }
}

/** Public + joined circles, deduplicated — a public circle the user joined appears once. */
private fun buildFilteredList(
    publicCircles: List<Circle>,
    myCircles: List<Circle>,
    query: String,
    status: CircleStatus?,
): List<Circle> = (publicCircles + myCircles)
    .distinctBy { it.id }
    .filter { circle ->
        (status == null || circle.status == status) &&
            (query.isBlank() || circle.name.contains(query, ignoreCase = true) ||
                circle.host?.displayName.orEmpty().contains(query, ignoreCase = true))
    }

private fun CircleListUiState.closeJoinSheet() = copy(
    joinSheetVisible = false,
    joinByToken = false,
    joinCircleId = "",
    joinPassword = "",
    joinToken = "",
    isJoining = false,
    joinErrorRes = null,
)
