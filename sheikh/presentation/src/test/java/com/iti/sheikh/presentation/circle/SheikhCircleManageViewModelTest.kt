package com.iti.sheikh.presentation.circle

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleMemberRole
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.meeting.domain.model.circle.PendingJoinRequest
import com.iti.meeting.domain.repository.CircleRosterEvent
import com.iti.meeting.domain.repository.PendingJoinRequestEvent
import com.iti.sheikh.presentation.circle.state.SheikhCircleManageIntent
import com.iti.sheikh.presentation.testing.FakeCircleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SheikhCircleManageViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads circle, pending requests, and members`() = runTest(dispatcher) {
        val viewModel = SheikhCircleManageViewModel(
            CIRCLE.id,
            FakeCircleRepository(
                myCircles = listOf(CIRCLE),
                pending = mapOf(CIRCLE.id to listOf(REQUEST)),
                members = mapOf(CIRCLE.id to listOf(HOST, MEMBER)),
            ),
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertEquals(CIRCLE, state.circle)
        assertEquals(listOf(REQUEST), state.pendingRequests)
        assertEquals(listOf(HOST, MEMBER), state.members)
    }

    @Test
    fun `surfaces error when the circle cannot be loaded`() = runTest(dispatcher) {
        val viewModel = SheikhCircleManageViewModel(
            CIRCLE.id,
            FakeCircleRepository(failCircle = true),
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isError)
    }

    @Test
    fun `approving a request removes it from pending and refreshes members`() = runTest(dispatcher) {
        val viewModel = SheikhCircleManageViewModel(
            CIRCLE.id,
            FakeCircleRepository(
                myCircles = listOf(CIRCLE),
                pending = mapOf(CIRCLE.id to listOf(REQUEST)),
                members = mapOf(CIRCLE.id to listOf(HOST)),
            ),
        )
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.pendingRequests.size)

        viewModel.onIntent(SheikhCircleManageIntent.ApproveRequest(REQUEST.userId))
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.pendingRequests.isEmpty())
        assertFalse(viewModel.state.value.actionInProgress)
    }

    @Test
    fun `live pending request is added to state`() = runTest(dispatcher) {
        val repository = FakeCircleRepository(myCircles = listOf(CIRCLE))
        val viewModel = SheikhCircleManageViewModel(CIRCLE.id, repository)
        testScheduler.advanceUntilIdle()

        repository.emitPendingRequestEvent(PendingJoinRequestEvent.Received(REQUEST))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(REQUEST), viewModel.state.value.pendingRequests)
    }

    @Test
    fun `roster started event refreshes the circle status`() = runTest(dispatcher) {
        val repository = FakeCircleRepository(myCircles = listOf(CIRCLE))
        val viewModel = SheikhCircleManageViewModel(CIRCLE.id, repository)
        testScheduler.advanceUntilIdle()
        assertEquals(CircleStatus.SCHEDULED, viewModel.state.value.circle?.status)

        repository.emitRosterEvent(CircleRosterEvent.Started)
        testScheduler.advanceUntilIdle()

        assertEquals(CircleStatus.ONGOING, viewModel.state.value.circle?.status)
    }

    @Test
    fun `start transition moves the circle to ongoing`() = runTest(dispatcher) {
        val viewModel = SheikhCircleManageViewModel(
            CIRCLE.id,
            FakeCircleRepository(myCircles = listOf(CIRCLE)),
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(SheikhCircleManageIntent.StartClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(CircleStatus.ONGOING, viewModel.state.value.circle?.status)
    }

    companion object {
        val HOST = CircleMember(
            id = "m-host",
            userId = "u-host",
            displayName = "Sheikh Ali",
            initials = "SA",
            role = CircleMemberRole.HOST,
        )
        val MEMBER = CircleMember(
            id = "m-member",
            userId = "u-member",
            displayName = "Student Omar",
            initials = "SO",
            role = CircleMemberRole.MEMBER,
        )
        val REQUEST = PendingJoinRequest(
            membershipId = "m-req",
            userId = "u-requester",
            displayName = "Student Khalid",
            initials = "SK",
        )
        val CIRCLE = Circle(
            id = "c1",
            name = "Evening Circle",
            startDate = "2026-08-03T18:00:00Z",
            status = CircleStatus.SCHEDULED,
            type = CircleType.PUBLIC,
            maxParticipants = 10,
            currentMembers = 2,
            host = HOST,
        )
    }
}
