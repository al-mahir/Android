package com.iti.presentation.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.getOrNull
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.presentation.circle.state.InSessionEffect
import com.iti.presentation.circle.state.InSessionIntent
import com.iti.presentation.circle.state.InSessionUiState
import com.iti.presentation.circle.state.SessionParticipant
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class InSessionViewModel(
    private val circleId: String,
    private val getCircles: GetStudyCirclesUseCase,
) : ViewModel(),
    StateHolder<InSessionUiState> by DefaultStateHolder(InSessionUiState()),
    EffectPublisher<InSessionEffect> by DefaultEffectPublisher() {

    init {
        observeCircle()
        seedParticipants()
    }

    fun onIntent(intent: InSessionIntent) = when (intent) {
        InSessionIntent.ToggleMic -> updateState { copy(isMicMuted = !isMicMuted) }
        InSessionIntent.ToggleRaiseHand -> updateState { copy(isHandRaised = !isHandRaised) }
        InSessionIntent.Leave -> sendEffect(InSessionEffect.NavigateBack)
        InSessionIntent.OpenChat -> updateState { copy(unreadChatCount = 0) }
        InSessionIntent.OpenMushaf -> sendEffect(InSessionEffect.OpenMushaf)
    }

    private fun observeCircle() {
        getCircles()
            .catch {}
            .onEach { result ->
                val circles = result.getOrNull() ?: return@onEach
                val circle = circles.firstOrNull { it.id == circleId }
                updateState { copy(circle = circle) }
            }
            .launchIn(viewModelScope)
    }

    private fun seedParticipants() {
        updateState {
            copy(
                participants = FAKE_PARTICIPANTS,
                speakingParticipantId = FAKE_PARTICIPANTS.firstOrNull()?.id,
            )
        }
    }

    private companion object {
        val FAKE_PARTICIPANTS = listOf(
            SessionParticipant("p1", "عمر", "عم", isSpeaking = true, isMuted = false),
            SessionParticipant("p2", "فاطمة", "فا"),
            SessionParticipant("p3", "محمد", "مح"),
            SessionParticipant("p4", "أنس", "أي"),
            SessionParticipant("p5", "سارة", "سا"),
            SessionParticipant("p6", "يحيى", "يا"),
        )
    }
}
