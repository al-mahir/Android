package com.iti.sheikh.presentation.testing

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleJoinResult
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleToken
import com.iti.meeting.domain.model.circle.CreateCircleRequest
import com.iti.meeting.domain.model.circle.PendingJoinRequest
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.meeting.domain.repository.CircleRosterEvent
import com.iti.meeting.domain.repository.JoinRequestEvent
import com.iti.meeting.domain.repository.PendingJoinRequestEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [CircleRepository] for the sheikh presentation tests — mirrors the real
 * Ktor/STOMP-backed `CircleRepositoryImpl` in `:meeting:data` (same return contract:
 * `kotlin.Result`, never thrown).
 */
class FakeCircleRepository(
    private val circles: List<Circle> = emptyList(),
    private val myCircles: List<Circle> = emptyList(),
    private val pending: Map<String, List<PendingJoinRequest>> = emptyMap(),
    private val members: Map<String, List<CircleMember>> = emptyMap(),
    var failMine: Boolean = false,
    private val failCircle: Boolean = false,
    private val failPending: Boolean = false,
    private val failMembers: Boolean = false,
    private val failApprove: Boolean = false,
    private val failReject: Boolean = false,
    private val failRemove: Boolean = false,
    private val failStart: Boolean = false,
    private val failEnd: Boolean = false,
    private val failCancel: Boolean = false,
    private val failLeave: Boolean = false,
    private val createdCircle: (CreateCircleRequest) -> Circle = {
        Circle(
            id = "created-1",
            name = it.name,
            startDate = it.startDate,
            endDate = it.endDate,
            type = it.type,
            requiresApproval = it.requiresApproval,
            maxParticipants = it.maxParticipants,
        )
    },
    private val startedCircle: (Circle) -> Circle = { it.copy(status = CircleStatus.ONGOING) },
) : CircleRepository {

    private val rosterEvents = MutableSharedFlow<CircleRosterEvent>(extraBufferCapacity = Int.MAX_VALUE)
    private val pendingEvents = MutableSharedFlow<PendingJoinRequestEvent>(extraBufferCapacity = Int.MAX_VALUE)

    fun emitRosterEvent(event: CircleRosterEvent) {
        if (event is CircleRosterEvent.Started) {
            circleStore.keys.toList().forEach { key ->
                circleStore[key] = startedCircle(circleStore.getValue(key))
            }
        }
        rosterEvents.tryEmit(event)
    }

    fun emitPendingRequestEvent(event: PendingJoinRequestEvent) {
        pendingEvents.tryEmit(event)
    }

    private fun find(circleId: String): Circle =
        circleStore.getValue(circleId)

    private val circleStore: MutableMap<String, Circle> =
        (circles + myCircles).associateBy { it.id }.toMutableMap()

    override suspend fun getPublicCircles(status: CircleStatus?): Result<List<Circle>> =
        Result.success(circles)

    override suspend fun getMyCircles(): Result<List<Circle>> =
        if (failMine) Result.failure(BOOM) else Result.success(myCircles)

    override suspend fun getCircle(circleId: String): Result<Circle> =
        if (failCircle) Result.failure(BOOM) else Result.success(find(circleId))

    override suspend fun createCircle(request: CreateCircleRequest): Result<Circle> =
        Result.success(createdCircle(request))

    override suspend fun joinCircle(circleId: String, password: String?): CircleJoinResult =
        CircleJoinResult.Joined("membership-1")

    override suspend fun cancelJoinRequest(circleId: String): Result<Unit> = Result.success(Unit)

    override suspend fun leaveCircle(circleId: String): Result<Unit> =
        if (failLeave) Result.failure(BOOM) else Result.success(Unit)

    override suspend fun approveJoinRequest(circleId: String, userId: String): Result<Unit> =
        if (failApprove) Result.failure(BOOM) else Result.success(Unit)

    override suspend fun rejectJoinRequest(circleId: String, userId: String): Result<Unit> =
        if (failReject) Result.failure(BOOM) else Result.success(Unit)

    override suspend fun getPendingRequests(circleId: String): Result<List<PendingJoinRequest>> =
        if (failPending) Result.failure(BOOM) else Result.success(pending[circleId].orEmpty())

    override suspend fun getMembers(circleId: String): Result<List<CircleMember>> =
        if (failMembers) Result.failure(BOOM) else Result.success(members[circleId].orEmpty())

    override suspend fun removeMember(circleId: String, userId: String): Result<Unit> =
        if (failRemove) Result.failure(BOOM) else Result.success(Unit)

    override suspend fun startCircle(circleId: String): Result<Circle> {
        if (failStart) return Result.failure(BOOM)
        val updated = startedCircle(find(circleId))
        circleStore[circleId] = updated
        return Result.success(updated)
    }

    override suspend fun endCircle(circleId: String): Result<Unit> =
        if (failEnd) Result.failure(BOOM) else Result.success(Unit)

    override suspend fun cancelCircle(circleId: String): Result<Unit> =
        if (failCancel) Result.failure(BOOM) else Result.success(Unit)

    override suspend fun getToken(circleId: String): Result<CircleToken> =
        Result.success(CircleToken(token = "", channelName = "", userAccount = ""))

    override fun observeCircleEvents(circleId: String): Flow<CircleRosterEvent> =
        rosterEvents

    override fun observeJoinRequestEvents(membershipId: String): Flow<JoinRequestEvent> =
        flowOf()

    override fun observePendingRequests(circleId: String): Flow<PendingJoinRequestEvent> =
        pendingEvents

    private companion object {
        val BOOM = IllegalStateException("boom")
    }
}
