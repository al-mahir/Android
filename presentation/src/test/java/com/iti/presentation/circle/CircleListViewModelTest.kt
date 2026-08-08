package com.iti.presentation.circle

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleJoinError
import com.iti.meeting.domain.model.circle.CircleJoinResult
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.presentation.R
import com.iti.presentation.circle.state.CircleListEffect
import com.iti.presentation.circle.state.CircleListIntent
import com.iti.presentation.circle.state.CircleListUiState
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
class CircleListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `public circles are loaded into the list`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeCircleRepository(circles = listOf(CIRCLE)))

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertEquals(listOf(CIRCLE), state.filteredCircles)
    }

    @Test
    fun `a circles failure surfaces as a full-screen error`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeCircleRepository(failPublic = true))

        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isError)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `status filter narrows the results`() = runTest(dispatcher) {
        val completed = CIRCLE.copy(id = "circle-2", name = "مكتملة", status = CircleStatus.COMPLETED)
        val viewModel = viewModel(FakeCircleRepository(circles = listOf(CIRCLE, completed)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleListIntent.StatusSelected(CircleStatus.COMPLETED))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(completed), viewModel.state.value.filteredCircles)
        assertEquals(CircleStatus.COMPLETED, viewModel.state.value.selectedStatus)
    }

    @Test
    fun `search query matches by circle name`() = runTest(dispatcher) {
        val other = CIRCLE.copy(id = "circle-2", name = "مراجعة الأجزاء")
        val viewModel = viewModel(FakeCircleRepository(circles = listOf(CIRCLE, other)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleListIntent.SearchQueryChanged("تجويد"))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(CIRCLE), viewModel.state.value.filteredCircles)
    }

    @Test
    fun `status filter combines with search`() = runTest(dispatcher) {
        val scheduled = CIRCLE.copy(id = "circle-2", name = "حفظ", status = CircleStatus.SCHEDULED)
        val completed = CIRCLE.copy(id = "circle-3", name = "حفظ", status = CircleStatus.COMPLETED)
        val viewModel = viewModel(FakeCircleRepository(circles = listOf(CIRCLE, scheduled, completed)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleListIntent.StatusSelected(CircleStatus.COMPLETED))
        viewModel.onIntent(CircleListIntent.SearchQueryChanged("حفظ"))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(completed), viewModel.state.value.filteredCircles)
    }

    @Test
    fun `joined circle ids are loaded for markers`() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakeCircleRepository(
                circles = listOf(CIRCLE),
                myCircles = listOf(CIRCLE.copy(id = "circle-2")),
            ),
        )
        testScheduler.advanceUntilIdle()

        val state: CircleListUiState = viewModel.state.value
        assertTrue("circle-2" in state.joinedCircleIds)
    }

    @Test
    fun `a circle click emits navigation carrying the circle id`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeCircleRepository(circles = listOf(CIRCLE)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleListIntent.CircleClicked("circle-1"))

        assertEquals(CircleListEffect.OpenCircle("circle-1"), viewModel.effect.first())
    }

    @Test
    fun `create circle click emits navigation to the create screen`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeCircleRepository())
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleListIntent.CreateCircleClicked)

        assertEquals(CircleListEffect.OpenCreateCircle, viewModel.effect.first())
    }

    @Test
    fun `joined private circles are merged into the list and featured as current`() = runTest(dispatcher) {
        val private = CIRCLE.copy(id = "private-1", name = "حلقة خاصة", type = CircleType.PRIVATE)
        val viewModel = viewModel(FakeCircleRepository(myCircles = listOf(private)))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.filteredCircles.any { it.id == "private-1" })
        assertEquals("private-1", state.currentCircle?.id)
    }

    @Test
    fun `a joined public circle is not duplicated in the list`() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakeCircleRepository(
                circles = listOf(CIRCLE),
                myCircles = listOf(CIRCLE.copy(id = "circle-1", currentMembers = 9)),
            ),
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.filteredCircles.count { it.id == "circle-1" })
    }

    @Test
    fun `open join sheet shows it`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeCircleRepository())
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleListIntent.JoinPrivateClicked)

        assertTrue(viewModel.state.value.joinSheetVisible)
    }

    @Test
    fun `submitting join with a blank id shows an inline error`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeCircleRepository())
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleListIntent.JoinPrivateClicked)
        viewModel.onIntent(CircleListIntent.SubmitJoinPrivate)
        testScheduler.advanceUntilIdle()

        assertEquals(R.string.circle_join_id_required, viewModel.state.value.joinErrorRes)
    }

    @Test
    fun `joining a private circle by id adds it to the list`() = runTest(dispatcher) {
        val private = CIRCLE.copy(id = "private-1", name = "حلقة خاصة", type = CircleType.PRIVATE)
        val viewModel = viewModel(FakeCircleRepository(privateCircles = listOf(private)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleListIntent.JoinPrivateClicked)
        viewModel.onIntent(CircleListIntent.JoinCircleIdChanged("private-1"))
        viewModel.onIntent(CircleListIntent.SubmitJoinPrivate)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.joinSheetVisible)
        assertTrue(state.filteredCircles.any { it.id == "private-1" })
        assertTrue("private-1" in state.joinedCircleIds)
    }

    @Test
    fun `a join failure keeps the sheet open and shows the error`() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakeCircleRepository(
                privateCircles = listOf(CIRCLE.copy(id = "private-1", type = CircleType.PRIVATE)),
                joinResult = { CircleJoinResult.Error(CircleJoinError.INVALID_PASSWORD, "wrong") },
            ),
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleListIntent.JoinPrivateClicked)
        viewModel.onIntent(CircleListIntent.JoinCircleIdChanged("private-1"))
        viewModel.onIntent(CircleListIntent.SubmitJoinPrivate)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.joinSheetVisible)
        assertEquals(R.string.circle_join_error_password, state.joinErrorRes)
        assertFalse(state.isJoining)
    }

    @Test
    fun `joining an already-joined circle closes the sheet and emits a message`() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakeCircleRepository(
                privateCircles = listOf(CIRCLE.copy(id = "private-1", type = CircleType.PRIVATE)),
                joinResult = { CircleJoinResult.Error(CircleJoinError.ALREADY_MEMBER, "already") },
            ),
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CircleListIntent.JoinPrivateClicked)
        viewModel.onIntent(CircleListIntent.JoinCircleIdChanged("private-1"))
        viewModel.onIntent(CircleListIntent.SubmitJoinPrivate)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.joinSheetVisible)
        assertEquals(
            CircleListEffect.ShowMessage(R.string.circle_join_error_already_member),
            viewModel.effect.first(),
        )
    }

    private fun viewModel(circleRepository: FakeCircleRepository) =
        CircleListViewModel(circleRepository = circleRepository)

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
