package com.iti.presentation.home

import com.iti.domain.connectivity.ConnectivityObserver
import com.iti.domain.connectivity.ConnectivityStatus
import com.iti.domain.repository.ReadingProgressRepository
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.usecase.reading.GetAyahOfTheDayUseCase
import com.iti.domain.usecase.reading.GetReadingProgressUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.presentation.home.state.HomeEffect
import com.iti.presentation.home.state.HomeIntent
import com.iti.presentation.testing.FakeAlmahirRepository
import com.iti.presentation.testing.FakeAppPreferencesRepository
import com.iti.presentation.testing.FakeCircleRepository
import com.iti.presentation.testing.FakeMeetingRepository
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
    fun `the resource streams are combined into one state`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAlmahirRepository(), circleRepository = FakeCircleRepository(myCircles = listOf(CIRCLE)))

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(null, state.errorMessageRes)
        assertEquals("JD", state.user?.initials)
        assertEquals(298, state.readingProgress?.pageNumber)
        assertEquals(1, state.sheikhs.size)
        assertEquals(1, state.myCircles.size)
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
    fun `a circles failure does not surface as a page error`() =
        runTest(dispatcher) {
            // My-circles loads as a separate one-shot call (like sheikhs); a partial failure
            // leaves the section empty instead of taking the whole screen down.
            val viewModel = viewModel(FakeAlmahirRepository(), circleRepository = FakeCircleRepository(failMine = true))

            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertFalse(state.hasError)
            assertTrue(state.myCircles.isEmpty())
        }

    @Test
    fun `available circles are loaded and filtered to joinable statuses`() =
        runTest(dispatcher) {
            val completed = CIRCLE.copy(id = "circle-2", name = "مكتملة", status = CircleStatus.COMPLETED)
            val viewModel = viewModel(
                FakeAlmahirRepository(),
                circleRepository = FakeCircleRepository(circles = listOf(CIRCLE, completed)),
            )

            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(1, state.availableCircles.size)
            assertEquals("circle-1", state.availableCircles.single().id)
        }

    @Test
    fun `a public-circles failure does not surface as a page error`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                FakeAlmahirRepository(),
                circleRepository = FakeCircleRepository(failPublic = true),
            )

            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertFalse(state.hasError)
            assertTrue(state.availableCircles.isEmpty())
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
    fun `see all circles intent emits navigation to the circle list`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAlmahirRepository())
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(HomeIntent.SeeAllCirclesClicked)

            assertEquals(HomeEffect.OpenCircleList, viewModel.effect.first())
        }

    @Test
    fun `circle click emits navigation carrying the circle id`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAlmahirRepository())
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(HomeIntent.CircleClicked("circle-1"))

        assertEquals(HomeEffect.OpenCircle("circle-1"), viewModel.effect.first())
    }

    private fun viewModel(
        repository: FakeAlmahirRepository,
        lastPage: Int = 298,
        failReadingProgress: Boolean = false,
        circleRepository: FakeCircleRepository = FakeCircleRepository(),
        appPreferencesRepository: com.iti.domain.settings.repository.AppPreferencesRepository = FakeAppPreferencesRepository(),
        connectivityObserver: com.iti.domain.connectivity.ConnectivityObserver = FakeConnectivityObserver(),
        meetingRepository: com.iti.meeting.domain.repository.MeetingRepository = FakeMeetingRepository(),
    ) = HomeViewModel(
        getCurrentUser = GetCurrentUserUseCase(appPreferencesRepository),
        getReadingProgress = GetReadingProgressUseCase(
            FakeReadingProgressRepository(lastPage, failReadingProgress),
        ),
        getAyahOfTheDay = GetAyahOfTheDayUseCase(),
        getSheikhs = GetSheikhsUseCase(repository),
        circleRepository = circleRepository,
        connectivityObserver = connectivityObserver,
        meetingRepository = meetingRepository,
    )

    private class FakeAppPreferencesRepository(
        user: com.iti.domain.model.User = FakeAlmahirRepository.USER
    ) : com.iti.domain.settings.repository.AppPreferencesRepository {
        val state = kotlinx.coroutines.flow.MutableStateFlow(com.iti.domain.settings.model.AppPreferences(user = user))
        override val preferences: Flow<com.iti.domain.settings.model.AppPreferences> = state
        override suspend fun setThemeMode(mode: com.iti.domain.settings.model.ThemeMode): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun setLanguage(language: com.iti.domain.settings.model.AppLanguage): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun setRemindersEnabled(enabled: Boolean): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun setErrorSoundsEnabled(enabled: Boolean): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun setDataSaverEnabled(enabled: Boolean): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun saveUser(user: com.iti.domain.model.User): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun clearUser(): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
    }

    private class FakeConnectivityObserver : com.iti.domain.connectivity.ConnectivityObserver {
        override val status: Flow<com.iti.domain.connectivity.ConnectivityStatus> = flowOf(com.iti.domain.connectivity.ConnectivityStatus.Available)
        override fun currentStatus(): com.iti.domain.connectivity.ConnectivityStatus = com.iti.domain.connectivity.ConnectivityStatus.Available
    }

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
        private val page: Int?,
        private val fail: Boolean = false,
    ) : ReadingProgressRepository {
        override fun observeLastPage(): Flow<Int?> =
            if (fail) flow { throw IllegalStateException("boom") } else flowOf(page)
    }

    companion object {
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
