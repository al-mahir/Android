package com.iti.sheikh.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.getOrNull
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SheikhHomeViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
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
        }
    }

    private fun observeContent() {
        contentJob?.cancel()
        updateState { copy(isLoading = true, errorMessageRes = null) }

        contentJob = getCurrentUser()
            .catch { updateState { copy(isLoading = false, errorMessageRes = R.string.sheikh_home_error_generic) } }
            .onEach { userResult ->
                val user = userResult.getOrNull()
                if (user == null) {
                    updateState { copy(isLoading = false, errorMessageRes = R.string.sheikh_home_error_generic) }
                    return@onEach
                }
                updateState {
                    copy(
                        isLoading = false,
                        errorMessageRes = null,
                        initials = user.initials,
                        avatarUrl = user.avatarUrl,
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
