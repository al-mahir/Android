package com.iti.sheikh.presentation.circle

import com.iti.meeting.domain.model.circle.CircleType
import com.iti.sheikh.presentation.circle.state.SheikhCreateCircleEffect
import com.iti.sheikh.presentation.circle.state.SheikhCreateCircleIntent
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SheikhCreateCircleViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `seeds default ISO start and end dates`() {
        val viewModel = SheikhCreateCircleViewModel(FakeCircleRepository())

        val state = viewModel.state.value
        assertTrue(state.startDate.isNotBlank())
        assertTrue(state.endDate.isNotBlank())
    }

    @Test
    fun `rejects blank name with a validation error`() = runTest(dispatcher) {
        val viewModel = SheikhCreateCircleViewModel(FakeCircleRepository())

        viewModel.onIntent(SheikhCreateCircleIntent.NameChanged("  "))
        viewModel.onIntent(SheikhCreateCircleIntent.Submit)
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.state.value.errorMessageRes)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `requires a password for private circles`() = runTest(dispatcher) {
        val viewModel = SheikhCreateCircleViewModel(FakeCircleRepository())

        viewModel.onIntent(SheikhCreateCircleIntent.TypeSelected(CircleType.PRIVATE))
        viewModel.onIntent(SheikhCreateCircleIntent.NameChanged("Private Circle"))
        viewModel.onIntent(SheikhCreateCircleIntent.Submit)
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.state.value.errorMessageRes)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `creates a valid circle and emits the created effect`() = runTest(dispatcher) {
        val viewModel = SheikhCreateCircleViewModel(FakeCircleRepository())

        viewModel.onIntent(SheikhCreateCircleIntent.NameChanged("Morning Circle"))
        viewModel.onIntent(SheikhCreateCircleIntent.TypeSelected(CircleType.PRIVATE))
        viewModel.onIntent(SheikhCreateCircleIntent.RequiresApprovalChanged(true))
        viewModel.onIntent(SheikhCreateCircleIntent.PasswordChanged("secret"))
        viewModel.onIntent(SheikhCreateCircleIntent.MaxParticipantsChanged("20"))
        viewModel.onIntent(SheikhCreateCircleIntent.Submit)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isSubmitting)
        assertNull(viewModel.state.value.errorMessageRes)

        val effect = viewModel.effect.first()
        assertTrue(effect is SheikhCreateCircleEffect.CircleCreated)
        assertEquals("created-1", (effect as SheikhCreateCircleEffect.CircleCreated).circleId)
    }

    @Test
    fun `strips non-digit characters from the capacity field`() {
        val viewModel = SheikhCreateCircleViewModel(FakeCircleRepository())

        viewModel.onIntent(SheikhCreateCircleIntent.MaxParticipantsChanged("1a2b3"))

        assertEquals("123", viewModel.state.value.maxParticipants)
    }
}
