package com.iti.presentation.circle

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleJoinError
import com.iti.meeting.domain.model.circle.CircleJoinResult
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.presentation.circle.state.CircleDetailsEffect
import com.iti.presentation.circle.state.CircleDetailsIntent
import com.iti.presentation.testing.FakeCircleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class CircleDetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a member is detected from my circles`() = runTest(dispatcher) {
        val viewModel = viewModel(circleRepository = FakeCircleRepository(circles = listOf(CIRCLE), myCircles = listOf(CIRCLE)))

        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isMember)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `a non-member is not marked as joined`() = runTest(dispatcher) {
        val viewModel = viewModel(circleRepository = FakeCircleRepository(circles = listOf(CIRCLE)))

        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isMember)
    }

    @Test
    fun `a membership-check failure leaves the screen joinable`() = runTest(dispatcher) {
        val viewModel = viewModel(circleRepository = FakeCircleRepository(circles = listOf(CIRCLE), failMine = true))

        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isMember)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `enter intent emits session navigation`() = runTest(dispatcher) {
        val viewModel = viewModel(circleRepository = FakeCircleRepository(circles = listOf(CIRCLE)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleDetailsIntent.EnterClicked)

        assertEquals(CircleDetailsEffect.OpenSession("circle-1"), viewModel.effect.first())
    }

    @Test
    fun `an already-member join error flips the screen to member state`() = runTest(dispatcher) {
        val viewModel = viewModel(
            circleRepository = FakeCircleRepository(
                circles = listOf(CIRCLE),
                joinResult = { CircleJoinResult.Error(CircleJoinError.ALREADY_MEMBER, "already a member") },
            ),
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleDetailsIntent.JoinClicked)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isMember)
        assertEquals(CircleDetailsEffect.ShowMessage(com.iti.presentation.R.string.circle_join_error_already_member), viewModel.effect.first())
    }

    @Test
    fun `leave clicked opens the confirmation dialog`() = runTest(dispatcher) {
        val viewModel = viewModel(circleRepository = FakeCircleRepository(circles = listOf(CIRCLE), myCircles = listOf(CIRCLE)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleDetailsIntent.LeaveClicked)

        assertTrue(viewModel.state.value.isLeaveDialogVisible)
        assertFalse(viewModel.state.value.isLeaving)
    }

    @Test
    fun `dismissing the leave dialog closes it`() = runTest(dispatcher) {
        val viewModel = viewModel(circleRepository = FakeCircleRepository(circles = listOf(CIRCLE), myCircles = listOf(CIRCLE)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleDetailsIntent.LeaveClicked)
        viewModel.onIntent(CircleDetailsIntent.DismissLeaveDialog)

        assertFalse(viewModel.state.value.isLeaveDialogVisible)
    }

    @Test
    fun `confirming leave removes membership and reports success`() = runTest(dispatcher) {
        val viewModel = viewModel(circleRepository = FakeCircleRepository(circles = listOf(CIRCLE), myCircles = listOf(CIRCLE)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleDetailsIntent.LeaveClicked)
        viewModel.onIntent(CircleDetailsIntent.ConfirmLeave)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isMember)
        assertFalse(viewModel.state.value.isLeaveDialogVisible)
        assertFalse(viewModel.state.value.isLeaving)
        assertEquals(
            CircleDetailsEffect.ShowMessage(com.iti.presentation.R.string.circle_leave_success),
            viewModel.effect.first(),
        )
    }

    @Test
    fun `a failed leave keeps membership and reports error`() = runTest(dispatcher) {
        val viewModel = viewModel(
            circleRepository = FakeCircleRepository(circles = listOf(CIRCLE), myCircles = listOf(CIRCLE), failLeave = true),
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleDetailsIntent.LeaveClicked)
        viewModel.onIntent(CircleDetailsIntent.ConfirmLeave)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isMember)
        assertFalse(viewModel.state.value.isLeaveDialogVisible)
        assertEquals(
            CircleDetailsEffect.ShowMessage(com.iti.presentation.R.string.circle_leave_error),
            viewModel.effect.first(),
        )
    }

    private fun viewModel(circleRepository: FakeCircleRepository) =
        CircleDetailsViewModel(circleId = "circle-1", circleRepository = circleRepository)

    private companion object {
        private val CIRCLE = Circle(
            id = "circle-1",
            name = "دورة التجويد",
            startDate = "2026-07-14T18:00:00Z",
            status = CircleStatus.SCHEDULED,
            maxParticipants = 15,
            currentMembers = 8,
            host = CircleMember(id = "m-1", userId = "sheikh-1", displayName = "Omar", initials = "عم"),
        )
    }
}
