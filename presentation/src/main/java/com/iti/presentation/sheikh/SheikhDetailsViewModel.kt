package com.iti.presentation.sheikh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.fold
import com.iti.domain.core.getOrNull
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.domain.usecase.sheikh.GetSheikhByIdUseCase
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.sheikh.state.SheikhDetailsEffect
import com.iti.presentation.sheikh.state.SheikhDetailsIntent
import com.iti.presentation.sheikh.state.SheikhDetailsUiState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SheikhDetailsViewModel(
    private val sheikhId: String,
    private val getSheikhById: GetSheikhByIdUseCase,
    private val getStudyCircles: GetStudyCirclesUseCase,
    private val joinCircle: JoinStudyCircleUseCase,
) : ViewModel(),
    StateHolder<SheikhDetailsUiState> by DefaultStateHolder(SheikhDetailsUiState()),
    EffectPublisher<SheikhDetailsEffect> by DefaultEffectPublisher() {

    init {
        load()
    }

    fun onIntent(intent: SheikhDetailsIntent) = when (intent) {
        SheikhDetailsIntent.Retry -> load()
        is SheikhDetailsIntent.JoinCircle -> join(intent.circleId)
    }

    private fun load() {
        // Load sheikh from real API (suspend)
        viewModelScope.launch {
            updateState { copy(isLoading = true, isError = false) }
            getSheikhById(sheikhId).fold(
                onSuccess = { sheikh ->
                    updateState {
                        copy(
                            sheikh = sheikh,
                            isLoading = false,
                            isError = sheikh == null,
                        )
                    }
                },
                onError = {
                    updateState { copy(isLoading = false, isError = true) }
                },
            )
        }

        // Observe circles from fake/reactive source (Flow)
        getStudyCircles()
            .catch { /* circles are non-critical; swallow errors silently */ }
            .onEach { result ->
                val circles = result.getOrNull() ?: return@onEach
                updateState { copy(circles = circles.filter { it.hostId == sheikhId }) }
            }
            .launchIn(viewModelScope)
    }

    private fun join(circleId: String) {
        viewModelScope.launch {
            joinCircle(circleId)
            sendEffect(SheikhDetailsEffect.NavigateToJoiningCircle(circleId))
        }
    }
}
