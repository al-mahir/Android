package com.iti.sheikh.presentation.availability

import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.domain.model.SheikhAvailabilityStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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


    suspend fun start(): Result<Unit> {
        if (job?.isActive == true) return Result.success(Unit)
        val firstPing = repository.setMyAvailability(SheikhAvailabilityStatus.AVAILABLE)
        job = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS.milliseconds)
                repository.setMyAvailability(SheikhAvailabilityStatus.AVAILABLE)
            }
        }
        return firstPing
    }

        @OptIn(DelicateCoroutinesApi::class)
    fun stop() {
        job?.cancel()
        job = null
        GlobalScope.launch(Dispatchers.IO) { repository.setMyAvailability(SheikhAvailabilityStatus.OFFLINE) }
    }

    fun pause() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MS = 20_000L
    }
}
