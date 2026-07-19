package com.iti.presentation.home

import com.iti.domain.model.ReadingProgress
import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.model.StudyCircle
import com.iti.domain.model.User
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.domain.usecase.reading.GetReadingProgressUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.presentation.home.state.HomeEffect
import com.iti.presentation.home.state.HomeIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
        val viewModel = viewModel(FakeRepository())

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
            val viewModel = viewModel(FakeRepository(readingProgress = null))

            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertNull(state.readingProgress)
            assertEquals(1, state.sheikhs.size)
        }

    @Test
    fun `one failing resource surfaces the error instead of hanging on loading`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeRepository(failSheikhs = true))

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
            val viewModel = viewModel(FakeRepository(readingProgress = null))
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
        getCurrentUser = GetCurrentUserUseCase(repository),
        getReadingProgress = GetReadingProgressUseCase(repository),
        getSheikhs = GetSheikhsUseCase(repository),
        getStudyCircles = GetStudyCirclesUseCase(repository),
        joinStudyCircle = JoinStudyCircleUseCase(repository),
    )

    private class FakeRepository(
        private val readingProgress: ReadingProgress? = PROGRESS,
        private val failSheikhs: Boolean = false,
    ) : AlmahirRepository {
        val joined = mutableListOf<String>()
        private val circles = MutableStateFlow(listOf(CIRCLE))

        override fun observeCurrentUser(): Flow<User> = flowOf(USER)

        override fun observeReadingProgress(): Flow<ReadingProgress?> = flowOf(readingProgress)

        override fun observeSheikhs(): Flow<List<Sheikh>> =
            if (failSheikhs) flow { throw IllegalStateException("boom") } else flowOf(listOf(SHEIKH))

        override fun observeStudyCircles(): Flow<List<StudyCircle>> = circles

        override suspend fun joinStudyCircle(circleId: String) {
            joined += circleId
        }
    }

    private companion object {
        val USER = User(
            id = "user-1",
            displayName = "Jamal Darwish",
            initials = "JD",
            avatarUrl = null,
        )
        val PROGRESS = ReadingProgress(surahName = "Al-Kahf", ayahNumber = 45, pageNumber = 298)
        val SHEIKH = Sheikh(
            id = "sheikh-1",
            name = "الشيخ أحمد",
            initials = "أح",
            avatarUrl = null,
            rating = 4.9,
            availability = SheikhAvailability.IN_SESSION,
        )
        val CIRCLE = StudyCircle(
            id = "circle-1",
            title = "دورة",
            hostName = "Omar",
            isJoined = false,
        )
    }
}
