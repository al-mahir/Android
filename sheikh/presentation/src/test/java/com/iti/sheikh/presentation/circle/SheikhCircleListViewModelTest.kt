package com.iti.sheikh.presentation.circle

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleMemberRole
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.sheikh.presentation.circle.state.SheikhCircleListEffect
import com.iti.sheikh.presentation.circle.state.SheikhCircleListIntent
import com.iti.sheikh.presentation.testing.FakeCircleRepository
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
class SheikhCircleListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads my circles into state`() = runTest(dispatcher) {
        val viewModel = SheikhCircleListViewModel(
            FakeCircleRepository(myCircles = listOf(CIRCLE)),
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertEquals(listOf(CIRCLE), state.circles)
    }

    @Test
    fun `surfaces error state when getMyCircles fails`() = runTest(dispatcher) {
        val viewModel = SheikhCircleListViewModel(
            FakeCircleRepository(failMine = true),
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isError)
        assertTrue(state.circles.isEmpty())
    }

    @Test
    fun `retry reloads after a failure`() = runTest(dispatcher) {
        val repository = FakeCircleRepository(myCircles = listOf(CIRCLE), failMine = true)
        val viewModel = SheikhCircleListViewModel(repository)

        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isError)

        repository.failMine = false
        viewModel.onIntent(SheikhCircleListIntent.Retry)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isError)
        assertEquals(listOf(CIRCLE), state.circles)
    }

    @Test
    fun `circle click emits open-circle effect`() = runTest(dispatcher) {
        val viewModel = SheikhCircleListViewModel(FakeCircleRepository(myCircles = listOf(CIRCLE)))

        viewModel.onIntent(SheikhCircleListIntent.CircleClicked(CIRCLE.id))

        assertEquals(SheikhCircleListEffect.OpenCircle(CIRCLE.id), viewModel.effect.first())
    }

    @Test
    fun `create click emits open-create-circle effect`() = runTest(dispatcher) {
        val viewModel = SheikhCircleListViewModel(FakeCircleRepository(myCircles = listOf(CIRCLE)))

        viewModel.onIntent(SheikhCircleListIntent.CreateCircleClicked)

        assertEquals(SheikhCircleListEffect.OpenCreateCircle, viewModel.effect.first())
    }

    companion object {
        val CIRCLE = Circle(
            id = "c1",
            name = "Evening Circle",
            startDate = "2026-08-03T18:00:00Z",
            status = CircleStatus.SCHEDULED,
            type = CircleType.PUBLIC,
            maxParticipants = 10,
            currentMembers = 2,
            host = CircleMember(
                id = "m-host",
                userId = "u-host",
                displayName = "Sheikh Ali",
                initials = "SA",
                role = CircleMemberRole.HOST,
            ),
        )
    }
}
