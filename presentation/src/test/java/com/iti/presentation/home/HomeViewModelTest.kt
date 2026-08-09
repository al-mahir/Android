package com.iti.presentation.home

import com.iti.domain.repository.ReadingProgressRepository
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.domain.usecase.reading.GetAyahOfTheDayUseCase
import com.iti.domain.usecase.reading.GetReadingProgressUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.presentation.home.state.HomeEffect
import com.iti.presentation.home.state.HomeIntent
import com.iti.presentation.testing.FakeAlmahirRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
        assertEquals(null, state.errorMessageRes)
        assertEquals("JD", state.user?.initials)
        assertEquals(298, state.readingProgress?.pageNumber)
        assertEquals(1, state.sheikhs.size)
        assertEquals(1, state.circles.size)
    }

    @Test
    fun `a brand-new reader still renders the rest of the screen`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAlmahirRepository(), lastPage = 1)

            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals(1, state.readingProgress?.pageNumber)
            assertEquals(1, state.sheikhs.size)
        }

    @Test
    fun `a sheikh-only failure does not surface as a page error`() =
        runTest(dispatcher) {
            // HomeViewModel loads sheikhs as a separate one-shot call and deliberately ignores
            // a partial sheikh failure so the rest of the screen still renders.
            val viewModel = viewModel(FakeAlmahirRepository(failSheikhs = true))

            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertFalse(state.hasError)
        }

    @Test
    fun `a failure in the core content streams surfaces the error instead of hanging on loading`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAlmahirRepository(), failReadingProgress = true)

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

    private fun viewModel(
        repository: FakeAlmahirRepository,
        appPreferencesRepo: com.iti.presentation.testing.FakeAppPreferencesRepository = com.iti.presentation.testing.FakeAppPreferencesRepository(),
        connectivityObserver: com.iti.presentation.testing.FakeConnectivityObserver = com.iti.presentation.testing.FakeConnectivityObserver(),
        meetingRepository: FakeMeetingRepository = FakeMeetingRepository(),
        lastPage: Int = 298,
        failReadingProgress: Boolean = false,
    ) = HomeViewModel(
        getCurrentUser = GetCurrentUserUseCase(appPreferencesRepo),
        getReadingProgress = GetReadingProgressUseCase(
            FakeReadingProgressRepository(lastPage, failReadingProgress),
        ),
        getAyahOfTheDay = GetAyahOfTheDayUseCase(),
        getSheikhs = GetSheikhsUseCase(repository),
        getStudyCircles = GetStudyCirclesUseCase(repository),
        joinStudyCircle = JoinStudyCircleUseCase(repository),
        connectivityObserver = connectivityObserver,
        meetingRepository = meetingRepository,
    )

    private class FakeMeetingRepository : com.iti.meeting.domain.repository.MeetingRepository {
        override val reconnected: Flow<Unit> = flowOf()
        override suspend fun getAvailableSheikhs(): kotlin.Result<List<com.iti.domain.model.MeetingSheikhSummary>> = kotlin.Result.success(emptyList())
        override suspend fun setMyAvailability(status: com.iti.domain.model.SheikhAvailabilityStatus): kotlin.Result<Unit> = kotlin.Result.success(Unit)
        override suspend fun getSheikhAvailability(sheikhId: String): kotlin.Result<com.iti.meeting.domain.model.SheikhAvailability> =
            kotlin.Result.success(com.iti.meeting.domain.model.SheikhAvailability(sheikhId, com.iti.domain.model.SheikhAvailabilityStatus.AVAILABLE, null))
        override suspend fun sendMeetingRequest(sheikhId: String, sheikhName: String?, note: String?): com.iti.meeting.domain.repository.SendMeetingRequestResult = com.iti.meeting.domain.repository.SendMeetingRequestResult.SheikhNotFound
        override suspend fun cancelMeetingRequest(requestId: String): kotlin.Result<Unit> = kotlin.Result.success(Unit)
        override suspend fun acceptMeetingRequest(requestId: String): kotlin.Result<com.iti.meeting.domain.model.MeetingRequestAccepted> = kotlin.Result.failure(Exception())
        override suspend fun declineMeetingRequest(requestId: String): kotlin.Result<Unit> = kotlin.Result.success(Unit)
        override suspend fun endMeeting(requestId: String): kotlin.Result<Unit> = kotlin.Result.success(Unit)
        override suspend fun refreshToken(requestId: String): kotlin.Result<com.iti.meeting.domain.model.TokenRefresh> = kotlin.Result.failure(Exception())
        override fun observePendingRequest(): Flow<com.iti.meeting.domain.model.PendingMeetingRequest?> = flowOf(null)
        override suspend fun getPendingRequest(): com.iti.meeting.domain.model.PendingMeetingRequest? = null
        override suspend fun clearPendingRequest() = Unit
        override fun observeMeetingRequestEvents(requestId: String): Flow<com.iti.meeting.domain.repository.MeetingRequestEvent> = flowOf()
        override fun observeIncomingRequests(sheikhId: String): Flow<com.iti.meeting.domain.repository.IncomingRequestEvent> = flowOf()
        override fun observeActiveCall(): Flow<com.iti.meeting.domain.model.ActiveCallRecord?> = flowOf(null)
        override suspend fun getActiveCall(): com.iti.meeting.domain.model.ActiveCallRecord? = null
        override suspend fun saveActiveCall(record: com.iti.meeting.domain.model.ActiveCallRecord) = Unit
        override suspend fun clearActiveCall() = Unit
    }

    private class FakeReadingProgressRepository(
        private val page: Int,
        private val fail: Boolean = false,
    ) : ReadingProgressRepository {
        override fun observeLastPage(): Flow<Int> =
            if (fail) flow { throw IllegalStateException("boom") } else flowOf(page)
    }
}

