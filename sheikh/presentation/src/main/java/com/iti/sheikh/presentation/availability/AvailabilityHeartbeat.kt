package com.iti.sheikh.presentation.availability

import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.domain.model.SheikhAvailabilityStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drives the sheikh's AVAILABLE status as a repeating heartbeat rather than a single API call —
 * the backend TTLs the status (~45s) so a killed app auto-reverts to OFFLINE without needing an
 * explicit call. See docs/Android-Agora-Implementation.md §10.1.
 */
class AvailabilityHeartbeat(
    private val repository: MeetingRepository,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun start(sheikhId: String) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                repository.setMyAvailability(sheikhId, SheikhAvailabilityStatus.AVAILABLE)
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    fun stop(sheikhId: String) {
        job?.cancel()
        job = null
        scope.launch { repository.setMyAvailability(sheikhId, SheikhAvailabilityStatus.OFFLINE) }
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MS = 20_000L
    }
}






