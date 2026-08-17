package com.iti.presentation.circle

import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.presentation.circle.CircleAudioStatus
import com.iti.meeting.presentation.circle.CircleAudioSessionState
import com.iti.meeting.presentation.circle.CircleParticipantAudio
import com.iti.presentation.circle.state.InSessionEffect
import com.iti.presentation.circle.state.InSessionIntent
import com.iti.presentation.testing.FakeCircleAudioSession
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

    @Test
    fun `joins the circle audio once the mic permission is granted`() = runTest(dispatcher) {
        val audioSession = FakeCircleAudioSession()
        val viewModel = viewModel(
            circleRepository = FakeCircleRepository(circles = listOf(CIRCLE)),
            audioSession = audioSession,
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(InSessionIntent.MicPermissionResult(granted = true))
        testScheduler.advanceUntilIdle()

        assertEquals("circle-1", audioSession.joinedCircleId)
        assertFalse(viewModel.state.value.isMicPermissionDenied)
    }

    @Test
    fun `a denied mic permission still joins so the member can listen`() = runTest(dispatcher) {
        val audioSession = FakeCircleAudioSession()
        val viewModel = viewModel(
            circleRepository = FakeCircleRepository(circles = listOf(CIRCLE)),
            audioSession = audioSession,
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(InSessionIntent.MicPermissionResult(granted = false))
        testScheduler.advanceUntilIdle()

        assertEquals("circle-1", audioSession.joinedCircleId)
        assertTrue(viewModel.state.value.isMicPermissionDenied)
    }

    @Test
    fun `a scheduled circle reports not-started instead of attempting a join`() = runTest(dispatcher) {
        val audioSession = FakeCircleAudioSession()
        val viewModel = viewModel(
            circleRepository = FakeCircleRepository(circles = listOf(CIRCLE.copy(status = CircleStatus.SCHEDULED))),
            audioSession = audioSession,
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(InSessionIntent.MicPermissionResult(granted = true))
        testScheduler.advanceUntilIdle()

        assertEquals(CircleAudioStatus.NotStarted, viewModel.state.value.audioStatus)
        assertEquals(0, audioSession.joinCount)
    }

    @Test
    fun `leaving the circle also leaves the audio channel`() = runTest(dispatcher) {
        val audioSession = FakeCircleAudioSession()
        val viewModel = viewModel(
            circleRepository = FakeCircleRepository(circles = listOf(CIRCLE)),
            audioSession = audioSession,
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(InSessionIntent.Leave)
        viewModel.onIntent(InSessionIntent.ConfirmLeave)
        testScheduler.advanceUntilIdle()

        assertEquals(1, audioSession.leaveCount)
    }

    @Test
    fun `live audio state is merged onto the roster`() = runTest(dispatcher) {
        val audioSession = FakeCircleAudioSession()
        val viewModel = viewModel(
            circleRepository = FakeCircleRepository(circles = listOf(CIRCLE), members = mapOf("circle-1" to listOf(MEMBER))),
            audioSession = audioSession,
        )
        testScheduler.advanceUntilIdle()

        audioSession.emit(
            CircleAudioSessionState(
                circleId = "circle-1",
                status = CircleAudioStatus.Live,
                remoteParticipants = mapOf(
                    42 to CircleParticipantAudio(uid = 42, userAccount = "student-1", isMicEnabled = true, isSpeaking = true),
                ),
            )
        )
        testScheduler.advanceUntilIdle()

        val participant = viewModel.state.value.participants.single { it.id == "student-1" }
        assertTrue(participant.isConnected)
        assertTrue(participant.isSpeaking)
        assertFalse(participant.isMuted)
    }

    @Test
    fun `a roster member with no agora stream reads as not connected`() = runTest(dispatcher) {
        val audioSession = FakeCircleAudioSession()
        val viewModel = viewModel(
            circleRepository = FakeCircleRepository(circles = listOf(CIRCLE), members = mapOf("circle-1" to listOf(MEMBER))),
            audioSession = audioSession,
        )
        testScheduler.advanceUntilIdle()

        audioSession.emit(CircleAudioSessionState(circleId = "circle-1", status = CircleAudioStatus.Live))
        testScheduler.advanceUntilIdle()

        val participant = viewModel.state.value.participants.single { it.id == "student-1" }
        assertFalse(participant.isConnected)
        assertTrue(participant.isMuted)
    }

    private fun viewModel(
        circleRepository: FakeCircleRepository,
        audioSession: FakeCircleAudioSession = FakeCircleAudioSession(),
    ) = InSessionViewModel(
        circleId = "circle-1",
        circleRepository = circleRepository,
        currentUserProvider = { "student-1" },
        audioSession = audioSession,
    )

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

        private val MEMBER = CircleMember(
            id = "m-2",
            userId = "student-1",
            displayName = "Layla",
            initials = "لى",
        )
    }
}
