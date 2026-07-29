package com.iti.meeting.domain.repository

import com.iti.domain.model.MeetingSheikhSummary
import com.iti.domain.model.SheikhAvailabilityStatus
import com.iti.meeting.domain.model.MeetingRequestAccepted
// Removing Instant from kotlinx.datetime since it's not strictly needed for domain if we use String, but let's use String to keep it simple.
import kotlinx.coroutines.flow.Flow

sealed interface SendMeetingRequestResult {
    data class Pending(val requestId: String, val expiresAt: String) : SendMeetingRequestResult
    data object SheikhUnavailable : SendMeetingRequestResult
    data object SheikhNotFound : SendMeetingRequestResult
    data class Error(val message: String) : SendMeetingRequestResult
}

sealed interface MeetingRequestEvent {
    data class Accepted(val circleId: String, val agoraToken: String, val channelName: String, val uid: Int) : MeetingRequestEvent
    data class Declined(val reason: String?) : MeetingRequestEvent
    data object Expired : MeetingRequestEvent
}

sealed interface IncomingRequestEvent {
    data class Received(val requestId: String, val studentName: String, val note: String, val expiresAt: String) : IncomingRequestEvent
    data class Cancelled(val requestId: String) : IncomingRequestEvent
}

interface MeetingRepository {
    suspend fun getAvailableSheikhs(): Result<List<MeetingSheikhSummary>>
    suspend fun setMyAvailability(sheikhId: String, status: SheikhAvailabilityStatus): Result<Unit>
    suspend fun sendMeetingRequest(sheikhId: String, note: String?): SendMeetingRequestResult
    suspend fun cancelMeetingRequest(requestId: String): Result<Unit>
    suspend fun acceptMeetingRequest(requestId: String): Result<MeetingRequestAccepted>
    suspend fun declineMeetingRequest(requestId: String, reason: String?): Result<Unit>
    fun observeMeetingRequestEvents(studentId: String, requestId: String): Flow<MeetingRequestEvent>
    fun observeIncomingRequests(sheikhId: String): Flow<IncomingRequestEvent>
}

