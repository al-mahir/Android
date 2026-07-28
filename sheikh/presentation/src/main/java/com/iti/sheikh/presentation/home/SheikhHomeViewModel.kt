package com.iti.sheikh.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.fold
import com.iti.domain.core.getOrNull
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.usecase.sheikh.ObserveMyAvailabilityUseCase
import com.iti.domain.usecase.sheikh.SetMyAvailabilityUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
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
    private val observeMyAvailability: ObserveMyAvailabilityUseCase,
    private val setMyAvailability: SetMyAvailabilityUseCase,
) : ViewModel(),
    StateHolder<SheikhHomeUiState> by DefaultStateHolder(SheikhHomeUiState()),
    EffectPublisher<SheikhHomeEffect> by DefaultEffectPublisher() {

    private var contentJob: Job? = null

    init {
        observeContent()
    }

    fun onIntent(intent: SheikhHomeIntent) {
        when (intent) {
            SheikhHomeIntent.ProfileClicked -> sendEffect(SheikhHomeEffect.OpenProfile)
            SheikhHomeIntent.Retry -> observeContent()
            is SheikhHomeIntent.AvailabilityToggled -> toggleAvailability(intent.isAvailable)
        }
    }

    private fun observeContent() {
        contentJob?.cancel()
        updateState { copy(isLoading = true, errorMessageRes = null) }

        contentJob = combine(
            getCurrentUser(),
            observeMyAvailability(),
        ) { userResult, availabilityResult ->
            val user = userResult.getOrNull() ?: error("Failed to load current user")
            val availability = availabilityResult.getOrNull() ?: error("Failed to load availability")
            user to availability
        }
            .catch { updateState { copy(isLoading = false, errorMessageRes = R.string.sheikh_home_error_generic) } }
            .onEach { (user, availability) ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessageRes = null,
                        initials = user.initials,
                        avatarUrl = user.avatarUrl,
                        availability = availability,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun toggleAvailability(isAvailable: Boolean) {
        val previous = currentState.availability
        val target = if (isAvailable) SheikhAvailability.AVAILABLE else SheikhAvailability.OFFLINE
        updateState { copy(availability = target, isUpdatingAvailability = true) }

        viewModelScope.launch {
            setMyAvailability(target).fold(
                onSuccess = { updateState { copy(isUpdatingAvailability = false) } },
                onError = {
                    updateState { copy(availability = previous, isUpdatingAvailability = false) }
                    sendEffect(SheikhHomeEffect.ShowMessage(R.string.sheikh_home_availability_update_failed))
                },
            )
        }
    }
}
