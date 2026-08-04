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

    private fun load(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) updateState { copy(isLoading = true, isError = false) }
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
                    // A silent background refresh failing (e.g. transient network blip) shouldn't
                    // blow away an already-loaded list into an error screen.
                    if (!silent) updateState { copy(isLoading = false, isError = true) }
                },
            )
        }
    }


    private fun pollAvailability() {
        viewModelScope.launch {
            while (isActive) {
                delay(AVAILABILITY_POLL_INTERVAL_MS)
                load(silent = true)
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
