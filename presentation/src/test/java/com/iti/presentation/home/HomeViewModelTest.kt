package com.iti.presentation.home

import com.iti.domain.model.home.ContinueReading
import com.iti.domain.model.home.HomeSummary
import com.iti.domain.model.home.Sheikh
import com.iti.domain.model.home.SheikhAvailability
import com.iti.domain.model.home.StudyCircle
import com.iti.domain.model.home.UserSummary
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.usecase.home.GetHomeSummaryUseCase
import com.iti.domain.usecase.home.JoinCircleUseCase
import com.iti.presentation.home.state.HomeEffect
import com.iti.presentation.home.state.HomeIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `emitted summary populates state and clears loading`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeRepository())

        // Let the init-block collection run.
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessageRes)
        assertEquals("JD", state.user?.initials)
        assertEquals(298, state.continueReading?.pageNumber)
        assertEquals(1, state.sheikhs.size)
        assertEquals(1, state.circles.size)
    }

    @Test
    fun `a failing stream surfaces the error message instead of hanging on loading`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FailingRepository())

            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertTrue(state.hasError)
        }

    @Test
    fun `continue reading intent emits navigation carrying the saved page`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeRepository())
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(HomeIntent.ContinueReadingClicked)

            assertEquals(HomeEffect.OpenMushafAtPage(298), viewModel.effect.first())
        }

    @Test
    fun `continue reading intent is ignored when there is no saved position`() =
        runTest(dispatcher) {
            val repository = FakeRepository(summary = SUMMARY.copy(continueReading = null))
            val viewModel = viewModel(repository)
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(HomeIntent.ContinueReadingClicked)
            // Nothing to navigate to, so no effect and no crash.
            viewModel.onIntent(HomeIntent.SearchClicked)

            assertEquals(HomeEffect.OpenSearch, viewModel.effect.first())
        }

    @Test
    fun `joining marks the circle pending and clears it once the call returns`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val viewModel = viewModel(repository)
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(HomeIntent.JoinCircleClicked("circle-1"))
            assertTrue("circle-1" in viewModel.state.value.joiningCircleIds)

            testScheduler.advanceUntilIdle()
            assertFalse("circle-1" in viewModel.state.value.joiningCircleIds)
            assertEquals(listOf("circle-1"), repository.joined)
        }

    @Test
    fun `a repeated tap while the join is in flight does not send a second request`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val viewModel = viewModel(repository)
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(HomeIntent.JoinCircleClicked("circle-1"))
            viewModel.onIntent(HomeIntent.JoinCircleClicked("circle-1"))
            testScheduler.advanceUntilIdle()

            assertEquals(listOf("circle-1"), repository.joined)
        }

    private fun viewModel(repository: AlmahirRepository) = HomeViewModel(
        getHomeSummary = GetHomeSummaryUseCase(repository),
        joinCircle = JoinCircleUseCase(repository),
    )

    private open class FakeRepository(summary: HomeSummary = SUMMARY) : AlmahirRepository {
        val joined = mutableListOf<String>()
        private val state = MutableStateFlow(summary)

        override fun observeHomeSummary(): Flow<HomeSummary> = state

        override suspend fun joinCircle(circleId: String) {
            joined += circleId
        }
    }

    private class FailingRepository : AlmahirRepository {
        override fun observeHomeSummary(): Flow<HomeSummary> = flow { throw IllegalStateException("boom") }

        override suspend fun joinCircle(circleId: String) = Unit
    }

    private companion object {
        val SUMMARY = HomeSummary(
            user = UserSummary(displayName = "Jamal Darwish", initials = "JD", avatarUrl = null),
            continueReading = ContinueReading(surahName = "Al-Kahf", ayahNumber = 45, pageNumber = 298),
            sheikhs = listOf(
                Sheikh(
                    id = "sheikh-1",
                    name = "الشيخ أحمد",
                    initials = "أح",
                    avatarUrl = null,
                    rating = 4.9,
                    availability = SheikhAvailability.IN_SESSION,
                ),
            ),
            circles = listOf(
                StudyCircle(id = "circle-1", title = "دورة", hostName = "Omar", isJoined = false),
            ),
        )
    }
}
