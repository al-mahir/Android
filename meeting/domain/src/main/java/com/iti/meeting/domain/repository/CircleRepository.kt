package com.iti.meeting.domain.repository

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleToken
import com.iti.meeting.domain.model.circle.CircleJoinResult
import com.iti.meeting.domain.model.circle.CreateCircleRequest
import com.iti.meeting.domain.model.circle.PendingJoinRequest
import kotlinx.coroutines.flow.Flow

/** Lifecycle/roster events on `/topic/circles/{circleId}` — see [CircleRepository.observeCircleEvents]. */
sealed interface CircleRosterEvent {
    data class MemberJoined(val member: CircleMember) : CircleRosterEvent
    data class MemberLeft(val userId: String) : CircleRosterEvent
    data class MemberRemoved(val userId: String) : CircleRosterEvent
    data object Started : CircleRosterEvent
    data object Ended : CircleRosterEvent
    data object Cancelled : CircleRosterEvent
}

/** Decision on the requester's own join request (`/topic/circle-memberships/{membershipId}`). */
sealed interface JoinRequestEvent {
    data class Approved(val member: CircleMember) : JoinRequestEvent
    data class Rejected(val reason: String?) : JoinRequestEvent
}

/** Events about pending join requests, delivered to the circle owner on
 * `/topic/circles/{circleId}/requests`. */
sealed interface PendingJoinRequestEvent {
    data class Received(val request: PendingJoinRequest) : PendingJoinRequestEvent
    data class Removed(val membershipId: String) : PendingJoinRequestEvent
}

interface CircleRepository {
    suspend fun getPublicCircles(status: CircleStatus? = null): Result<List<Circle>>
    suspend fun getMyCircles(): Result<List<Circle>>
    suspend fun getCircle(circleId: String): Result<Circle>
    suspend fun createCircle(request: CreateCircleRequest): Result<Circle>
    suspend fun joinCircle(circleId: String, password: String? = null): CircleJoinResult
    suspend fun cancelJoinRequest(circleId: String): Result<Unit>
    suspend fun approveJoinRequest(circleId: String, userId: String): Result<Unit>
    suspend fun rejectJoinRequest(circleId: String, userId: String): Result<Unit>
    suspend fun getPendingRequests(circleId: String): Result<List<PendingJoinRequest>>
    suspend fun getMembers(circleId: String): Result<List<CircleMember>>
    suspend fun removeMember(circleId: String, userId: String): Result<Unit>
    suspend fun startCircle(circleId: String): Result<Circle>
    suspend fun endCircle(circleId: String): Result<Unit>
    suspend fun cancelCircle(circleId: String): Result<Unit>
    suspend fun getToken(circleId: String): Result<CircleToken>

    /** Roster + lifecycle for a circle the caller already belongs to (members and owner). */
    fun observeCircleEvents(circleId: String): Flow<CircleRosterEvent>

    /** Tracks the caller's own join request (subscribes once `joinCircle` returns a membershipId). */
    fun observeJoinRequestEvents(membershipId: String): Flow<JoinRequestEvent>

    /** Live pending join requests for a circle the caller owns. */
    fun observePendingRequests(circleId: String): Flow<PendingJoinRequestEvent>
}
