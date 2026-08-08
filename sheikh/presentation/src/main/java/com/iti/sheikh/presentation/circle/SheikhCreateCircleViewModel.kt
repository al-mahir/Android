package com.iti.sheikh.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.meeting.domain.model.circle.CreateCircleRequest
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.sheikh.presentation.R
import com.iti.sheikh.presentation.circle.state.SheikhCreateCircleEffect
import com.iti.sheikh.presentation.circle.state.SheikhCreateCircleIntent
import com.iti.sheikh.presentation.circle.state.SheikhCreateCircleUiState
import com.iti.sheikh.presentation.core.mvi.DefaultEffectPublisher
import com.iti.sheikh.presentation.core.mvi.DefaultStateHolder
import com.iti.sheikh.presentation.core.mvi.EffectPublisher
import com.iti.sheikh.presentation.core.mvi.StateHolder
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SheikhCreateCircleViewModel(
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<SheikhCreateCircleUiState> by DefaultStateHolder(SheikhCreateCircleUiState()),
    EffectPublisher<SheikhCreateCircleEffect> by DefaultEffectPublisher() {

    init {
        val now = Clock.System.now().epochSeconds
        updateState {
            copy(
                startDate = Instant.fromEpochSeconds(now + 3_600).toString(),
                endDate = Instant.fromEpochSeconds(now + 7_200).toString(),
            )
        }
    }

    fun onIntent(intent: SheikhCreateCircleIntent) = when (intent) {
        is SheikhCreateCircleIntent.NameChanged -> updateState { copy(name = intent.name, errorMessageRes = null) }
        is SheikhCreateCircleIntent.TypeSelected -> updateState { copy(type = intent.type, errorMessageRes = null) }
        is SheikhCreateCircleIntent.RequiresApprovalChanged -> updateState { copy(requiresApproval = intent.value, errorMessageRes = null) }
        is SheikhCreateCircleIntent.MaxParticipantsChanged ->
            updateState { copy(maxParticipants = intent.value.filter { it.isDigit() }.take(3), errorMessageRes = null) }
        is SheikhCreateCircleIntent.PasswordChanged -> updateState { copy(password = intent.password, errorMessageRes = null) }
        is SheikhCreateCircleIntent.StartDateChanged -> updateState { copy(startDate = intent.value, errorMessageRes = null) }
        is SheikhCreateCircleIntent.EndDateChanged -> updateState { copy(endDate = intent.value, errorMessageRes = null) }
        SheikhCreateCircleIntent.Submit -> submit()
    }

    private fun submit() {
        val state = currentState
        if (state.isSubmitting) return

        val capacity = state.maxParticipants.toIntOrNull() ?: 0
        val validationError = when {
            state.name.isBlank() -> R.string.sheikh_create_circle_error_name
            state.startDate.isBlank() -> R.string.sheikh_create_circle_error_start_date
            capacity < 1 -> R.string.sheikh_create_circle_error_capacity
            state.type == CircleType.PRIVATE && state.password.isBlank() -> R.string.sheikh_create_circle_error_password
            else -> null
        }
        if (validationError != null) {
            updateState { copy(errorMessageRes = validationError) }
            return
        }

        updateState { copy(isSubmitting = true, errorMessageRes = null) }
        viewModelScope.launch {
            circleRepository.createCircle(
                CreateCircleRequest(
                    name = state.name.trim(),
                    startDate = state.startDate.trim(),
                    endDate = state.endDate.trim().ifBlank { null },
                    type = state.type,
                    requiresApproval = state.requiresApproval,
                    maxParticipants = capacity,
                    password = state.password.trim().ifBlank { null },
                ),
            ).fold(
                onSuccess = { circle ->
                    updateState { copy(isSubmitting = false) }
                    sendEffect(SheikhCreateCircleEffect.CircleCreated(circle.id))
                },
                onFailure = {
                    updateState { copy(isSubmitting = false) }
                    sendEffect(SheikhCreateCircleEffect.ShowMessage(R.string.sheikh_create_circle_error_generic))
                },
            )
        }
    }
}
