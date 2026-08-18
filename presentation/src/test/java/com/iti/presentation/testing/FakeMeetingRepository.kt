package com.iti.presentation.testing

import com.iti.domain.model.MeetingSheikhSummary
import com.iti.domain.model.SheikhAvailabilityStatus
import com.iti.meeting.domain.model.ActiveCallRecord
import com.iti.meeting.domain.model.MeetingRequestAccepted
import com.iti.meeting.domain.model.PendingMeetingRequest
import com.iti.meeting.domain.model.SheikhAvailability
import com.iti.meeting.domain.model.TokenRefresh
import com.iti.meeting.domain.repository.IncomingRequestEvent
import com.iti.meeting.domain.repository.MeetingRepository
import com.iti.meeting.domain.repository.MeetingRequestEvent
import com.iti.meeting.domain.repository.SendMeetingRequestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [MeetingRepository] for the presentation tests — deliberately simple, with the
 * locally-cached pending request / active call exposed as mutable state so tests can drive the
 * home screen's meeting surfaces.
 */
class FakeMeetingRepository(
    private val pendingRequest: PendingMeetingRequest? = null,
    private val activeCall: ActiveCallRecord? = null,
) : MeetingRepository {

    private val pendingFlow = MutableStateFlow(pendingRequest)
    private val activeFlow = MutableStateFlow(activeCall)

    override val reconnected: Flow<Unit> = flowOf()

    override suspend fun getAvailableSheikhs(): Result<List<MeetingSheikhSummary>> =
        Result.success(emptyList())

    override suspend fun setMyAvailability(status: SheikhAvailabilityStatus): Result<Unit> =
        Result.success(Unit)

    override suspend fun getSheikhAvailability(sheikhId: String): Result<SheikhAvailability> =
        Result.success(SheikhAvailability(sheikhId, SheikhAvailabilityStatus.AVAILABLE, null))

    override suspend fun sendMeetingRequest(
        sheikhId: String,
        sheikhName: String?,
        note: String?,
    ): SendMeetingRequestResult = SendMeetingRequestResult.Error("unsupported in fake")

    override suspend fun cancelMeetingRequest(requestId: String): Result<Unit> {
        pendingFlow.value = null
        return Result.success(Unit)
    }

    override suspend fun acceptMeetingRequest(requestId: String): Result<MeetingRequestAccepted> =
        Result.failure(IllegalStateException("unsupported in fake"))

    override suspend fun declineMeetingRequest(requestId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun endMeeting(requestId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshToken(requestId: String): Result<TokenRefresh> =
        Result.success(TokenRefresh("token", "channel", "account"))

    override fun observePendingRequest(): Flow<PendingMeetingRequest?> = pendingFlow

    override suspend fun getPendingRequest(): PendingMeetingRequest? = pendingFlow.value

    override suspend fun clearPendingRequest() {
        pendingFlow.value = null
    }

    override fun observeActiveCall(): Flow<ActiveCallRecord?> = activeFlow

    override suspend fun getActiveCall(): ActiveCallRecord? = activeFlow.value

    override suspend fun saveActiveCall(record: ActiveCallRecord) {
        activeFlow.value = record
    }

    override suspend fun clearActiveCall() {
        activeFlow.value = null
    }

    override fun observeMeetingRequestEvents(requestId: String): Flow<MeetingRequestEvent> = flowOf()

    override fun observeIncomingRequests(sheikhId: String): Flow<IncomingRequestEvent> = flowOf()
}
