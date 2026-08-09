package com.iti.presentation.testing

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleJoinResult
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleToken
import com.iti.meeting.domain.model.circle.CreateCircleRequest
import com.iti.meeting.domain.model.circle.PendingJoinRequest
import com.iti.meeting.domain.model.circle.UpdateCircleRequest
import com.iti.meeting.domain.repository.CircleRepository
import com.iti.meeting.domain.repository.CircleRosterEvent
import com.iti.meeting.domain.repository.JoinRequestEvent
import com.iti.meeting.domain.repository.PendingJoinRequestEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [CircleRepository] for the presentation tests — mirrors the real Ktor/STOMP-backed
 * `CircleRepositoryImpl` in `:meeting:data` (same return contract: `kotlin.Result`, never thrown).
 */
class FakeCircleRepository(
    private val circles: List<Circle> = emptyList(),
    private val myCircles: List<Circle> = emptyList(),
    private val privateCircles: List<Circle> = emptyList(),
    private val members: Map<String, List<CircleMember>> = emptyMap(),
    private val failPublic: Boolean = false,
    private val failMine: Boolean = false,
    private val failCircle: Boolean = false,
    private val failLeave: Boolean = false,
    private val joinResult: (Circle) -> CircleJoinResult = { CircleJoinResult.Joined("membership-1") },
) : CircleRepository {

    private val rosterEvents = MutableStateFlow<List<CircleRosterEvent>>(emptyList())
    private val joinRequestEvents = MutableStateFlow<List<JoinRequestEvent>>(emptyList())

    /** Circles the current user has joined; grows when [joinCircle] succeeds. */
    private val joinedCircles = myCircles.toMutableList()

    fun emitRosterEvent(event: CircleRosterEvent) {
        rosterEvents.value = rosterEvents.value + event
    }

    fun emitJoinRequestEvent(event: JoinRequestEvent) {
        joinRequestEvents.value = joinRequestEvents.value + event
    }

    override suspend fun getPublicCircles(status: CircleStatus?): Result<List<Circle>> =
        if (failPublic) Result.failure(BOOM) else Result.success(circles)

    override suspend fun getMyCircles(): Result<List<Circle>> =
        if (failMine) Result.failure(BOOM) else Result.success(joinedCircles)

    override suspend fun getMyPrivateCircles(status: CircleStatus?): Result<List<Circle>> =
        Result.success(privateCircles.filter { status == null || it.status == status })

    override suspend fun getCircleHistory(): Result<List<Circle>> =
        Result.success(emptyList())

    override suspend fun getCircle(circleId: String): Result<Circle> {
        if (failCircle) return Result.failure(BOOM)
        return Result.success(find(circleId))
    }

    override suspend fun createCircle(request: CreateCircleRequest): Result<Circle> =
        Result.failure(BOOM)

    override suspend fun updateCircle(circleId: String, request: UpdateCircleRequest): Result<Circle> =
        Result.success(find(circleId))

    override suspend fun joinCircle(circleId: String, password: String?): CircleJoinResult {
        val result = joinResult(find(circleId))
        if (result is CircleJoinResult.Joined && joinedCircles.none { it.id == circleId }) {
            joinedCircles += find(circleId)
        }
        return result
    }

    override suspend fun joinCircleViaToken(token: String): CircleJoinResult {
        val result = CircleJoinResult.Joined("membership-token-1")
        return result
    }

    override suspend fun cancelJoinRequest(circleId: String): Result<Unit> = Result.success(Unit)

    override suspend fun leaveCircle(circleId: String): Result<Unit> {
        if (failLeave) return Result.failure(BOOM)
        joinedCircles.removeAll { it.id == circleId }
        return Result.success(Unit)
    }

    override suspend fun approveJoinRequest(circleId: String, userId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun rejectJoinRequest(circleId: String, userId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun getPendingRequests(circleId: String): Result<List<PendingJoinRequest>> =
        Result.success(emptyList())

    override suspend fun getMembers(circleId: String): Result<List<CircleMember>> =
        Result.success(members[circleId].orEmpty())

    override suspend fun removeMember(circleId: String, userId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun startCircle(circleId: String): Result<Circle> = Result.failure(BOOM)

    override suspend fun endCircle(circleId: String): Result<Unit> = Result.failure(BOOM)

    override suspend fun cancelCircle(circleId: String): Result<Unit> = Result.failure(BOOM)

    override suspend fun getToken(circleId: String): Result<CircleToken> = Result.failure(BOOM)

    override fun observeCircleEvents(circleId: String): Flow<CircleRosterEvent> =
        flowOf(*rosterEvents.value.toTypedArray())

    override fun observeJoinRequestEvents(membershipId: String): Flow<JoinRequestEvent> =
        flowOf(*joinRequestEvents.value.toTypedArray())

    override fun observePendingRequests(circleId: String): Flow<PendingJoinRequestEvent> =
        flowOf()

    private fun find(circleId: String): Circle =
        circles.firstOrNull { it.id == circleId }
            ?: privateCircles.firstOrNull { it.id == circleId }
            ?: joinedCircles.first { it.id == circleId }

    private companion object {
        val BOOM = IllegalStateException("boom")
    }
}
