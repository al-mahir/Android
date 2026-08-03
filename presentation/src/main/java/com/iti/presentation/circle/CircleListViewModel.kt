package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.presentation.circle.state.CircleListEffect
import com.iti.presentation.circle.state.CircleListIntent
import com.iti.presentation.circle.state.CircleListUiState
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.launch

class CircleListViewModel(
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<CircleListUiState> by DefaultStateHolder(CircleListUiState()),
    EffectPublisher<CircleListEffect> by DefaultEffectPublisher() {

    init {
        load()
    }

    fun onIntent(intent: CircleListIntent) = when (intent) {
        is CircleListIntent.SearchQueryChanged -> updateSearch(intent.query)
        is CircleListIntent.TypeSelected -> updateType(intent.type)
        is CircleListIntent.CircleClicked -> sendEffect(CircleListEffect.OpenCircle(intent.circleId))
        CircleListIntent.CreateCircleClicked -> sendEffect(CircleListEffect.OpenCreateCircle)
        CircleListIntent.Retry -> load()
    }

    private fun load() {
        updateState { copy(isLoading = true, isError = false) }
        viewModelScope.launch {
            circleRepository.getPublicCircles().fold(
                onSuccess = { circles ->
                    updateState {
                        copy(
                            circles = circles,
                            filteredCircles = circles.applyFilters(searchQuery, selectedType),
                            isLoading = false,
                            isError = false,
                        )
                    }
                },
                onFailure = { updateState { copy(isLoading = false, isError = true) } },
            )
        }
    }

    private fun updateSearch(query: String) {
        updateState {
            copy(
                searchQuery = query,
                filteredCircles = circles.applyFilters(query, selectedType),
            )
        }
    }

    private fun updateType(type: CircleType?) {
        updateState {
            copy(
                selectedType = type,
                filteredCircles = circles.applyFilters(searchQuery, type),
            )
        }
    }
}

private fun List<Circle>.applyFilters(query: String, type: CircleType?) =
    filter { circle ->
        (type == null || circle.type == type) &&
            (query.isBlank() || circle.name.contains(query, ignoreCase = true) ||
                circle.host?.displayName.orEmpty().contains(query, ignoreCase = true))
    }
