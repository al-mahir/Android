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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** A valid default schedule: start one hour from now, end an hour later. */
private fun defaultCreateCircleState(): CreateCircleUiState {
    val start = Instant.now().atZone(ZoneOffset.UTC).plusHours(1)
    return CreateCircleUiState(
        startDate = DateTimeFormatter.ISO_INSTANT.format(start.toInstant()),
        endDate = DateTimeFormatter.ISO_INSTANT.format(start.plusHours(1).toInstant()),
    )
}

class CreateCircleViewModel(
    private val circleRepository: CircleRepository,
) : ViewModel(),
    StateHolder<CreateCircleUiState> by DefaultStateHolder(defaultCreateCircleState()),
    EffectPublisher<CreateCircleEffect> by DefaultEffectPublisher() {

    fun onIntent(intent: CreateCircleIntent) = when (intent) {
        is CreateCircleIntent.TitleChanged -> updateState { copy(title = intent.value, titleError = false) }
        is CreateCircleIntent.GoalsChanged -> updateState { copy(goals = intent.value) }
        is CreateCircleIntent.PrivacySelected -> updateState { copy(selectedType = intent.type, password = "") }
        is CreateCircleIntent.PasswordChanged -> updateState { copy(password = intent.value, passwordError = false) }
        is CreateCircleIntent.StartDateChanged -> updateState { copy(startDate = intent.value, startDateError = false) }
        is CreateCircleIntent.EndDateChanged -> updateState { copy(endDate = intent.value, endDateError = false) }
        CreateCircleIntent.Submit -> submit()
    }

    private fun submit() {
        val state = currentState
        val titleBlank = state.title.isBlank()
        val passwordRequired = state.selectedType == CreateCirclePrivacyType.PRIVATE && state.password.isBlank()

        val now = Instant.now()
        val start = runCatching { Instant.parse(state.startDate.trim()) }.getOrNull()
        val end = runCatching { Instant.parse(state.endDate.trim()) }.getOrNull()
        val startDateInvalid = start == null || !start.isAfter(now)
        val endDateInvalid = end == null || (start != null && !end.isAfter(start))

        if (titleBlank || passwordRequired || startDateInvalid || endDateInvalid) {
            updateState {
                copy(
                    titleError = titleBlank,
                    passwordError = passwordRequired,
                    startDateError = startDateInvalid,
                    endDateError = endDateInvalid,
                )
            }
            return
        }

        if (state.isCreating) return
        updateState { copy(isCreating = true) }

        val request = CreateCircleRequest(
            name = state.title.trim(),
            startDate = state.startDate.trim(),
            endDate = state.endDate.trim(),
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
                    if (circle.type == CircleType.PRIVATE) {
                        // Private circles need the access code + id (and invite token, when
                        // available) to invite members.
                        sendEffect(CreateCircleEffect.ShowCreatedCircle(circle.id, state.password, circle.inviteToken))
                    } else {
                        sendEffect(CreateCircleEffect.CircleCreated(circle.id))
                    }
                },
                onFailure = {
                    updateState { copy(isCreating = false) }
                    sendEffect(CreateCircleEffect.ShowMessage(R.string.create_circle_error_generic))
                },
            )
        }
    }
}
