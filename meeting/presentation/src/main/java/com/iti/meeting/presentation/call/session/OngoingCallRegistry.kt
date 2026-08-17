package com.iti.meeting.presentation.call.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the ongoing-call notification renders.
 *
 * Deliberately a plain value type with no lambdas: it changes as often as the session does (a
 * volume-indication tick can fire several times a second), so it has to compare by value or the
 * `StateFlow` would push a notification rebuild on every emission. The actions live separately in
 * [OngoingCallActions] precisely so they can't break that equality.
 */
data class OngoingCallDisplay(
    val title: String?,
    val isMicEnabled: Boolean,
    val isConnecting: Boolean,
)

/** How the notification acts on the live session, whichever kind it is. */
data class OngoingCallActions(
    val onToggleMic: () -> Unit,
    val onHangUp: () -> Unit,
)

/**
 * The single place that knows whether *any* kind of call is live.
 *
 * Exists because [CallForegroundService] used to be hardwired to [CallSessionController], which made
 * it unusable for group circles. Both session controllers publish here instead, so there is one
 * ongoing-call notification and one foreground service regardless of session type — which matches
 * the hardware, since the device has exactly one microphone to hand out.
 */
class OngoingCallRegistry {

    private val _display = MutableStateFlow<OngoingCallDisplay?>(null)
    val display: StateFlow<OngoingCallDisplay?> = _display.asStateFlow()

    @Volatile
    private var actions: OngoingCallActions? = null

    /** Begins a session: publishes its first display state, wires its actions, and starts the
     * foreground service. */
    fun start(context: Context, display: OngoingCallDisplay, actions: OngoingCallActions) {
        this.actions = actions
        _display.value = display
        CallForegroundService.start(context.applicationContext)
    }

    /** Pushes new display state for the session already started. No-op once it has ended, so a
     * late callback from a torn-down engine can't resurrect the notification. */
    fun update(display: OngoingCallDisplay) {
        if (actions == null) return
        _display.value = display
    }

    fun toggleMic() = actions?.onToggleMic?.invoke()

    fun hangUp() = actions?.onHangUp?.invoke()

    fun stop(context: Context) {
        actions = null
        _display.value = null
        CallForegroundService.stop(context.applicationContext)
    }
}
