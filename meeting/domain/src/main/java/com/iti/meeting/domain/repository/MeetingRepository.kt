package com.iti.meeting.domain.repository

import com.iti.domain.model.MeetingSheikhSummary
import com.iti.domain.model.SheikhAvailabilityStatus
import com.iti.meeting.domain.model.ActiveCallRecord
import com.iti.meeting.domain.model.MeetingRequestAccepted
import com.iti.meeting.domain.model.PendingMeetingRequest
import com.iti.meeting.domain.model.SheikhAvailability
import com.iti.meeting.domain.model.TokenRefresh
import kotlinx.coroutines.flow.Flow

sealed interface SendMeetingRequestResult {
    data class Pending(val requestId: String, val channelName: String, val expiresAt: String) : SendMeetingRequestResult
    data object SheikhUnavailable : SendMeetingRequestResult
    data object SheikhNotFound : SendMeetingRequestResult
    data class AlreadyPending(val message: String) : SendMeetingRequestResult
    data class Error(val message: String) : SendMeetingRequestResult
}

/** Events on the unified `/topic/meeting-requests/{requestId}` destination — see [MeetingRepository.observeMeetingRequestEvents]. */
sealed interface MeetingRequestEvent {
    data class Accepted(val agoraToken: String, val channelName: String, val userAccount: String) : MeetingRequestEvent
    data class Declined(val reason: String?) : MeetingRequestEvent
    data object Cancelled : MeetingRequestEvent
    data object Expired : MeetingRequestEvent
    data object MeetingEnded : MeetingRequestEvent
}

sealed interface IncomingRequestEvent {
    data class Received(val requestId: String, val studentName: String, val note: String, val expiresAt: String) : IncomingRequestEvent
    data class Cancelled(val requestId: String) : IncomingRequestEvent
}

interface MeetingRepository {
    /** Fires whenever the STOMP connection re-establishes after a drop — callers should re-run their reconcile GET. */
    val reconnected: Flow<Unit>

    suspend fun getAvailableSheikhs(): Result<List<MeetingSheikhSummary>>
    suspend fun setMyAvailability(status: SheikhAvailabilityStatus): Result<Unit>
    suspend fun getSheikhAvailability(sheikhId: String): Result<SheikhAvailability>
    suspend fun sendMeetingRequest(sheikhId: String, sheikhName: String?, note: String?): SendMeetingRequestResult
    suspend fun cancelMeetingRequest(requestId: String): Result<Unit>
    suspend fun acceptMeetingRequest(requestId: String): Result<MeetingRequestAccepted>
    suspend fun declineMeetingRequest(requestId: String): Result<Unit>
    suspend fun endMeeting(requestId: String): Result<Unit>
    suspend fun refreshToken(requestId: String): Result<TokenRefresh>

    /** Locally-cached record of the current student's outstanding request — see
     * [PendingMeetingRequest]. Survives app restarts; the backend has no equivalent GET. */
    fun observePendingRequest(): Flow<PendingMeetingRequest?>
    suspend fun getPendingRequest(): PendingMeetingRequest?
    suspend fun clearPendingRequest()

    /** Locally-cached record of a call the current user had joined — see [ActiveCallRecord]. The
     * only thing that survives real process death mid-call; used to offer a "rejoin" prompt on
     * the next cold start. Written by `CallSessionController` on a successful join, cleared on
     * any terminal state. */
    fun observeActiveCall(): Flow<ActiveCallRecord?>
    suspend fun getActiveCall(): ActiveCallRecord?
    suspend fun saveActiveCall(record: ActiveCallRecord)
    suspend fun clearActiveCall()

    /**
     * The single unified topic for one request's lifecycle
     * (Accepted/Declined/Cancelled/Expired/MeetingEnded). Used by both the requesting student
     * (from the moment the request is sent) and the responding sheikh (from the moment it learns
     * the requestId, currently only reachable once [observeIncomingRequests] is resolved — see
     * docs/Meeting-Feature-Status.md).
     */
    fun observeMeetingRequestEvents(requestId: String): Flow<MeetingRequestEvent>

    /**
     * NOT CONFIRMED against the real backend — see [com.iti.meeting.data.remote.MeetingWsDestinations.sheikhRequests].
     */
    fun observeIncomingRequests(sheikhId: String): Flow<IncomingRequestEvent>
}
