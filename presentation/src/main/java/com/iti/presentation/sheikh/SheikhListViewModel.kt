package com.iti.presentation.sheikh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch

class SheikhListViewModel(
    private val getSheikhs: GetSheikhsUseCase,
) : ViewModel(),
    StateHolder<SheikhListUiState> by DefaultStateHolder(SheikhListUiState()),
    EffectPublisher<SheikhListEffect> by DefaultEffectPublisher() {

    init {
        load()
    }

    fun onIntent(intent: SheikhListIntent) = when (intent) {
        is SheikhListIntent.SearchQueryChanged -> updateSearch(intent.query)
        is SheikhListIntent.FilterSelected -> updateFilter(intent.filter)
        is SheikhListIntent.SheikhClicked -> sendEffect(SheikhListEffect.NavigateToSheikhDetails(intent.sheikhId))
        SheikhListIntent.Retry -> load()
    }

    private fun load() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, isError = false) }
            runCatching { getSheikhs() }
                .onSuccess { list ->
                    updateState {
                        copy(
                            sheikhs = list,
                            filteredSheikhs = list.applyFilters(searchQuery, selectedFilter),
                            isLoading = false,
                            isError = false,
                        )
                    }
                }
                .onFailure {
                    updateState { copy(isLoading = false, isError = true) }
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
}
