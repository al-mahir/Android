package com.iti.sheikh.presentation.availability

import androidx.lifecycle.ViewModel
import com.iti.sheikh.presentation.availability.state.AvailabilityEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Thin per-screen adapter over [SheikhAvailabilityController] — mirrors
 * [com.iti.meeting.presentation.call.CallViewModel]'s relationship to
 * `CallSessionController`. The controller (a process-lifetime Koin singleton) owns the actual
 * heartbeat/STOMP-subscription/accept/decline logic so it keeps running whether or not this
 * ViewModel (and the panel it's scoped to) is alive.
 */
class AvailabilityViewModel(
    private val controller: SheikhAvailabilityController,
) : ViewModel() {

    val state: StateFlow<AvailabilityUiState> = controller.state

    val effect: Flow<AvailabilityEffect> = controller.errors.map { AvailabilityEffect.ShowMessage(it) }

    fun onIntent(intent: AvailabilityIntent) {
        when (intent) {
            is AvailabilityIntent.ToggleAvailability ->
                if (intent.isAvailable) controller.goAvailable() else controller.goOffline()
            AvailabilityIntent.Accept ->
                (controller.currentState as? AvailabilityUiState.IncomingRequest)?.let { controller.accept(it.requestId) }
            AvailabilityIntent.Decline ->
                (controller.currentState as? AvailabilityUiState.IncomingRequest)?.let { controller.decline(it.requestId) }
        }
    }
}
