package com.iti.presentation.home

import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.domain.usecase.reading.GetReadingProgressUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.presentation.home.state.HomeEffect
import com.iti.presentation.home.state.HomeIntent
import com.iti.presentation.testing.FakeAlmahirRepository
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
    fun `the four resource streams are combined into one state`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAlmahirRepository())

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessageRes)
        assertEquals("JD", state.user?.initials)
        assertEquals(298, state.readingProgress?.pageNumber)
        assertEquals(1, state.sheikhs.size)
        assertEquals(1, state.circles.size)
    }

    @Test
    fun `a missing reading position still renders the rest of the screen`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAlmahirRepository(readingProgress = null))

            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertNull(state.readingProgress)
            assertEquals(1, state.sheikhs.size)
        }

    @Test
    fun `one failing resource surfaces the error instead of hanging on loading`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAlmahirRepository(failSheikhs = true))

            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertTrue(state.hasError)
        }

    @Test
    fun `continue reading intent emits navigation carrying the saved page`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAlmahirRepository())
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(HomeIntent.ContinueReadingClicked)

            assertEquals(HomeEffect.OpenMushafAtPage(298), viewModel.effect.first())
        }

    @Test
    fun `continue reading intent is ignored when there is no saved position`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAlmahirRepository(readingProgress = null))
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(HomeIntent.ContinueReadingClicked)
            // Nothing to navigate to, so no effect and no crash.
            viewModel.onIntent(HomeIntent.SearchClicked)

            assertEquals(HomeEffect.OpenSearch, viewModel.effect.first())
        }

    @Test
    fun `joining marks the circle pending and clears it once the call returns`() =
        runTest(dispatcher) {
            val repository = FakeAlmahirRepository()
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
            val repository = FakeAlmahirRepository()
            val viewModel = viewModel(repository)
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(HomeIntent.JoinCircleClicked("circle-1"))
            viewModel.onIntent(HomeIntent.JoinCircleClicked("circle-1"))
            testScheduler.advanceUntilIdle()

            assertEquals(listOf("circle-1"), repository.joined)
        }

    private fun viewModel(repository: AlmahirRepository) = HomeViewModel(
        getCurrentUser = GetCurrentUserUseCase(repository),
        getReadingProgress = GetReadingProgressUseCase(repository),
        getSheikhs = GetSheikhsUseCase(repository),
        getStudyCircles = GetStudyCirclesUseCase(repository),
        joinStudyCircle = JoinStudyCircleUseCase(repository),
    )

}
