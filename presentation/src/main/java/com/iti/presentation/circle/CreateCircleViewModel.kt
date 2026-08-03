package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.meeting.domain.model.circle.CreateCircleRequest
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.presentation.R
import com.iti.presentation.circle.state.CreateCircleEffect
import com.iti.presentation.circle.state.CreateCircleIntent
import com.iti.presentation.circle.state.CreateCirclePrivacyType
import com.iti.presentation.circle.state.CreateCircleUiState
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

class CreateCircleViewModel(
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<CreateCircleUiState> by DefaultStateHolder(CreateCircleUiState()),
    EffectPublisher<CreateCircleEffect> by DefaultEffectPublisher() {

    fun onIntent(intent: CreateCircleIntent) = when (intent) {
        is CreateCircleIntent.TitleChanged -> updateState { copy(title = intent.value, titleError = false) }
        is CreateCircleIntent.GoalsChanged -> updateState { copy(goals = intent.value) }
        is CreateCircleIntent.PrivacySelected -> updateState { copy(selectedType = intent.type, password = "") }
        is CreateCircleIntent.PasswordChanged -> updateState { copy(password = intent.value, passwordError = false) }
        CreateCircleIntent.Submit -> submit()
    }

    private fun submit() {
        val state = currentState
        val titleBlank = state.title.isBlank()
        val passwordRequired = state.selectedType == CreateCirclePrivacyType.PRIVATE && state.password.isBlank()

        if (titleBlank || passwordRequired) {
            updateState { copy(titleError = titleBlank, passwordError = passwordRequired) }
            return
        }

        if (state.isCreating) return
        updateState { copy(isCreating = true) }

        val now = Instant.now().atZone(java.time.ZoneOffset.UTC)
        val startDate = DateTimeFormatter.ISO_INSTANT.format(now.toInstant())

        val request = CreateCircleRequest(
            name = state.title.trim(),
            startDate = startDate,
            type = when (state.selectedType) {
                CreateCirclePrivacyType.PRIVATE -> CircleType.PRIVATE
                CreateCirclePrivacyType.PUBLIC -> CircleType.PUBLIC
            },
            requiresApproval = true,
            maxParticipants = 50,
            password = if (state.selectedType == CreateCirclePrivacyType.PRIVATE) state.password else null,
        )

        viewModelScope.launch {
            circleRepository.createCircle(request).fold(
                onSuccess = { circle ->
                    updateState { copy(isCreating = false) }
                    sendEffect(CreateCircleEffect.CircleCreated(circle.id))
                },
                onFailure = {
                    updateState { copy(isCreating = false) }
                    sendEffect(CreateCircleEffect.ShowMessage(R.string.create_circle_error_generic))
                },
            )
        }
    }
}
