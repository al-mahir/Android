package com.iti.sheikh.presentation.availability

import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.domain.model.SheikhAvailabilityStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


class AvailabilityHeartbeat(
    private val repository: MeetingRepository,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                repository.setMyAvailability(SheikhAvailabilityStatus.AVAILABLE)
                delay(HEARTBEAT_INTERVAL_MS.milliseconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        scope.launch { repository.setMyAvailability(SheikhAvailabilityStatus.OFFLINE) }
    }

    fun pause() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MS = 20_000L
    }
}
