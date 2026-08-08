package com.iti.presentation.circle

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.presentation.circle.state.InSessionEffect
import com.iti.presentation.circle.state.InSessionIntent
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
class InSessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `leave opens the confirmation dialog`() = runTest(dispatcher) {
        val viewModel = viewModel(circleRepository = FakeCircleRepository(circles = listOf(CIRCLE)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(InSessionIntent.Leave)

        assertTrue(viewModel.state.value.isLeaveDialogVisible)
        assertFalse(viewModel.state.value.isLeaving)
    }

    @Test
    fun `dismissing the leave dialog closes it`() = runTest(dispatcher) {
        val viewModel = viewModel(circleRepository = FakeCircleRepository(circles = listOf(CIRCLE)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(InSessionIntent.Leave)
        viewModel.onIntent(InSessionIntent.DismissLeaveDialog)

        assertFalse(viewModel.state.value.isLeaveDialogVisible)
    }

    @Test
    fun `confirming leave navigates back on success`() = runTest(dispatcher) {
        val viewModel = viewModel(circleRepository = FakeCircleRepository(circles = listOf(CIRCLE)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(InSessionIntent.Leave)
        viewModel.onIntent(InSessionIntent.ConfirmLeave)
        testScheduler.advanceUntilIdle()

        assertEquals(InSessionEffect.NavigateBack, viewModel.effect.first())
    }

    @Test
    fun `a failed leave reports an error and stays in the session`() = runTest(dispatcher) {
        val viewModel = viewModel(
            circleRepository = FakeCircleRepository(circles = listOf(CIRCLE), failLeave = true),
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(InSessionIntent.Leave)
        viewModel.onIntent(InSessionIntent.ConfirmLeave)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLeaveDialogVisible)
        assertFalse(viewModel.state.value.isLeaving)
        assertEquals(
            InSessionEffect.ShowMessage(com.iti.presentation.R.string.circle_leave_error),
            viewModel.effect.first(),
        )
    }

    private fun viewModel(circleRepository: FakeCircleRepository) =
        InSessionViewModel(circleId = "circle-1", circleRepository = circleRepository)

    private companion object {
        private val CIRCLE = Circle(
            id = "circle-1",
            name = "دورة التجويد",
            startDate = "2026-07-14T18:00:00Z",
            status = CircleStatus.ONGOING,
            maxParticipants = 15,
            currentMembers = 8,
            host = CircleMember(id = "m-1", userId = "sheikh-1", displayName = "Omar", initials = "عم"),
        )
    }
}
