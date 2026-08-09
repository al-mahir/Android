package com.iti.presentation.sheikh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.fold
import com.iti.domain.usecase.sheikh.GetSheikhByIdUseCase
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.sheikh.state.SheikhDetailsEffect
import com.iti.presentation.sheikh.state.SheikhDetailsIntent
import com.iti.presentation.sheikh.state.SheikhDetailsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SheikhDetailsViewModel(
    private val sheikhId: String,
    private val getSheikhById: GetSheikhByIdUseCase,
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<SheikhDetailsUiState> by DefaultStateHolder(SheikhDetailsUiState()),
    EffectPublisher<SheikhDetailsEffect> by DefaultEffectPublisher() {

    init {
        load()
        pollAvailability()
    }

    fun onIntent(intent: SheikhDetailsIntent) = when (intent) {
        SheikhDetailsIntent.Retry -> load()
        is SheikhDetailsIntent.CircleClicked -> sendEffect(SheikhDetailsEffect.OpenCircle(intent.circleId))
    }

    private fun load() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, isError = false) }
            fetchSheikh(silent = false)
            
            // The public listing has no host filter; the sheikh's own circles are the ones they
            // host. Circles are a section — a failure leaves it empty rather than failing the page.
            circleRepository.getPublicCircles().fold(
                onSuccess = { circles ->
                    updateState {
                        copy(circles = circles.filter { it.host?.userId == sheikhId })
                    }
                },
                onFailure = { /* circles are non-critical; ignore */ },
            )
        }
    }

    private suspend fun fetchSheikh(silent: Boolean) {
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
                if (!silent) updateState { copy(isLoading = false, isError = true) }
            },
        )
    }

    private fun pollAvailability() {
        viewModelScope.launch {
            while (isActive) {
                delay(AVAILABILITY_POLL_INTERVAL_MS)
                fetchSheikh(silent = true)
            }
        }
    }

    private companion object {
        const val AVAILABILITY_POLL_INTERVAL_MS = 20_000L
    }
}
