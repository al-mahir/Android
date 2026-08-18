package com.iti.presentation.sheikh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.Result
import com.iti.domain.core.fold
import com.iti.domain.model.Bookmark
import com.iti.domain.model.BookmarkType
import com.iti.domain.usecase.bookmark.ObserveBookmarksUseCase
import com.iti.domain.usecase.bookmark.ToggleBookmarkUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.sheikh.state.SheikhFilter
import com.iti.presentation.sheikh.state.SheikhListEffect
import com.iti.presentation.sheikh.state.SheikhListIntent
import com.iti.presentation.sheikh.state.SheikhListUiState
import com.iti.presentation.sheikh.state.applyFilters
import com.iti.presentation.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SheikhListViewModel(
    private val getSheikhs: GetSheikhsUseCase,
    private val observeBookmarks: ObserveBookmarksUseCase,
    private val toggleBookmark: ToggleBookmarkUseCase,
) : ViewModel(),
    StateHolder<SheikhListUiState> by DefaultStateHolder(SheikhListUiState()),
    EffectPublisher<SheikhListEffect> by DefaultEffectPublisher() {


    init {
        load()
        observeBookmarkedSheikhs()
        pollAvailability()
    }

    fun onIntent(intent: SheikhListIntent) = when (intent) {
        is SheikhListIntent.SearchQueryChanged -> updateSearch(intent.query)
        is SheikhListIntent.FilterSelected -> updateFilter(intent.filter)
        is SheikhListIntent.SheikhClicked -> sendEffect(SheikhListEffect.NavigateToSheikhDetails(intent.sheikhId))
        is SheikhListIntent.ToggleSheikhBookmark -> toggleSheikhBookmark(intent.sheikhId)
        SheikhListIntent.Retry -> load()
        SheikhListIntent.Refresh -> refresh()
    }

    private fun observeBookmarkedSheikhs() {
        observeBookmarks(BookmarkType.SHEIKH)
            .onEach { result ->
                if (result is Result.Success) {
                    updateState { copy(bookmarkedSheikhIds = result.data.mapNotNull { it.sheikhId }.toSet()) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun toggleSheikhBookmark(sheikhId: String) {
        viewModelScope.launch {
            toggleBookmark(
                Bookmark(
                    id = "",
                    type = BookmarkType.SHEIKH,
                    sheikhId = sheikhId,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun load() {
        viewModelScope.launch { fetchSheikhs(showSkeleton = true) }
    }

    /**
     * Swipe-to-refresh. Unlike [load] it never raises [SheikhListUiState.isLoading], so the list
     * the user is reading stays put and only the pull indicator spins. Re-entrant pulls are
     * dropped: the gesture can fire again before `isRefreshing` has reached the UI.
     */
    private fun refresh() {
        if (currentState.isRefreshing) return
        updateState { copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                fetchSheikhs(showSkeleton = false, reportFailure = true)
            } finally {
                // Also runs if the ViewModel is cleared mid-refresh, so the flag never sticks.
                updateState { copy(isRefreshing = false) }
            }
        }
    }

    /**
     * @param showSkeleton flips the whole screen into loading/error. Only the initial load and
     *   Retry do that; a pull or the availability poll keeps whatever is already on screen, so a
     *   transient blip cannot blow an already-loaded list away into an error screen.
     * @param reportFailure surfaces a message when the fetch fails. The user-initiated pull says
     *   so; the background poll stays quiet.
     */
    private suspend fun fetchSheikhs(showSkeleton: Boolean, reportFailure: Boolean = false) {
        if (showSkeleton) updateState { copy(isLoading = true, isError = false) }
        getSheikhs().fold(
            onSuccess = { list ->
                updateState {
                    copy(
                        sheikhs = list,
                        filteredSheikhs = list.applyFilters(searchQuery, selectedFilter),
                        isLoading = false,
                        isError = false,
                    )
                }
            },
            onError = {
                val hasContent = currentState.sheikhs.isNotEmpty()
                if (showSkeleton || !hasContent) {
                    updateState { copy(isLoading = false, isError = true) }
                } else if (reportFailure) {
                    sendEffect(SheikhListEffect.ShowMessage(R.string.refresh_failed))
                }
            },
        )
    }


    private fun pollAvailability() {
        viewModelScope.launch {
            while (isActive) {
                delay(AVAILABILITY_POLL_INTERVAL_MS)
                // Never while a pull is in flight — two writers would fight over the same list.
                if (!currentState.isRefreshing) fetchSheikhs(showSkeleton = false)
            }
        }
    }

    private fun updateSearch(query: String) {
        updateState {
            copy(
                searchQuery = query,
                filteredSheikhs = sheikhs.applyFilters(query, selectedFilter),
            )
        }
    }

    private fun updateFilter(filter: SheikhFilter) {
        updateState {
            copy(
                selectedFilter = filter,
                filteredSheikhs = sheikhs.applyFilters(searchQuery, filter),
            )
        }
    }

    private companion object {
        const val AVAILABILITY_POLL_INTERVAL_MS = 20_000L
    }
}
