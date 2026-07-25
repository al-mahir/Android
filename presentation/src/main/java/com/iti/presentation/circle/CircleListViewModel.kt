package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.presentation.circle.state.CircleListEffect
import com.iti.presentation.circle.state.CircleListIntent
import com.iti.presentation.circle.state.CircleListUiState
import com.iti.presentation.circle.state.CircleListUiState.Companion.TAG_ALL
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CircleListViewModel(
    private val getCircles: GetStudyCirclesUseCase,
    private val joinCircle: JoinStudyCircleUseCase,
) : ViewModel(),
    StateHolder<CircleListUiState> by DefaultStateHolder(CircleListUiState()),
    EffectPublisher<CircleListEffect> by DefaultEffectPublisher() {

    init {
        load()
    }

    fun onIntent(intent: CircleListIntent) = when (intent) {
        is CircleListIntent.SearchQueryChanged -> updateSearch(intent.query)
        is CircleListIntent.TagSelected -> updateTag(intent.tag)
        is CircleListIntent.JoinCircle -> join(intent.circleId)
        CircleListIntent.Retry -> load()
    }

    private fun load() {
        updateState { copy(isLoading = true, isError = false) }
        getCircles()
            .catch { updateState { copy(isLoading = false, isError = true) } }
            .onEach { circles ->
                val tags = listOf(TAG_ALL) +
                    circles.map { it.surahName }.distinct().sorted()
                updateState {
                    copy(
                        circles = circles,
                        filteredCircles = circles.applyFilters(searchQuery, selectedTag),
                        availableTags = tags,
                        isLoading = false,
                        isError = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateSearch(query: String) {
        updateState {
            copy(
                searchQuery = query,
                filteredCircles = circles.applyFilters(query, selectedTag),
            )
        }
    }

    private fun updateTag(tag: String) {
        updateState {
            copy(
                selectedTag = tag,
                filteredCircles = circles.applyFilters(searchQuery, tag),
            )
        }
    }

    private fun join(circleId: String) {
        viewModelScope.launch {
            runCatching { joinCircle(circleId) }
            sendEffect(CircleListEffect.NavigateToJoiningCircle(circleId))
        }
    }
}

private fun List<com.iti.domain.model.StudyCircle>.applyFilters(query: String, tag: String) =
    filter { circle ->
        (tag == TAG_ALL || circle.surahName == tag) &&
            (query.isBlank() || circle.surahName.contains(query, ignoreCase = true) ||
                circle.hostName.contains(query, ignoreCase = true))
    }
