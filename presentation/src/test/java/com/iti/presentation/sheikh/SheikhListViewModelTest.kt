package com.iti.presentation.sheikh

import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.usecase.bookmark.ObserveBookmarksUseCase
import com.iti.domain.usecase.bookmark.ToggleBookmarkUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import com.iti.presentation.R
import com.iti.presentation.sheikh.state.SheikhFilter
import com.iti.presentation.sheikh.state.SheikhListEffect
import com.iti.presentation.sheikh.state.SheikhListIntent
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SheikhListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the sheikh list is loaded on start`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAlmahirRepository(sheikhs = listOf(AVAILABLE, BUSY)))

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertEquals(2, state.filteredSheikhs.size)
    }

    // ── Swipe-to-refresh ─────────────────────────────────────────────────

    @Test
    fun `a pull to refresh re-fetches without blanking the screen`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAlmahirRepository(sheikhs = listOf(AVAILABLE)))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(SheikhListIntent.Refresh)

        // The indicator spins while the list stays on screen — no skeleton, no error.
        val refreshing = viewModel.state.value
        assertTrue(refreshing.isRefreshing)
        assertFalse(refreshing.isLoading)
        assertEquals(listOf(AVAILABLE), refreshing.filteredSheikhs)

        testScheduler.advanceUntilIdle()

        val settled = viewModel.state.value
        assertFalse(settled.isRefreshing)
        assertFalse(settled.isError)
        assertEquals(listOf(AVAILABLE), settled.filteredSheikhs)
    }

    @Test
    fun `a pull to refresh keeps the active filter applied`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAlmahirRepository(sheikhs = listOf(AVAILABLE, BUSY)))
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(SheikhListIntent.FilterSelected(SheikhFilter.BUSY))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(SheikhListIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(BUSY), viewModel.state.value.filteredSheikhs)
    }

    @Test
    fun `a failed pull to refresh keeps the loaded list and reports the failure`() = runTest(dispatcher) {
        val repository = FakeAlmahirRepository(sheikhs = listOf(AVAILABLE))
        val viewModel = viewModel(repository)
        testScheduler.advanceUntilIdle()

        repository.failSheikhs = true
        viewModel.onIntent(SheikhListIntent.Refresh)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("a transient refresh blip must not blow the list away", state.isError)
        assertFalse(state.isRefreshing)
        assertEquals(listOf(AVAILABLE), state.filteredSheikhs)
        assertEquals(SheikhListEffect.ShowMessage(R.string.refresh_failed), viewModel.effect.first())
    }

    @Test
    fun `a failed pull to refresh on an empty list falls through to the error screen`() = runTest(dispatcher) {
        val repository = FakeAlmahirRepository(sheikhs = emptyList())
        val viewModel = viewModel(repository)
        testScheduler.advanceUntilIdle()

        repository.failSheikhs = true
        viewModel.onIntent(SheikhListIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isError)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `a second pull while one is already refreshing is ignored`() = runTest(dispatcher) {
        val repository = FakeAlmahirRepository(sheikhs = listOf(AVAILABLE))
        val viewModel = viewModel(repository)
        testScheduler.advanceUntilIdle()
        val callsBefore = repository.sheikhCalls

        viewModel.onIntent(SheikhListIntent.Refresh)
        viewModel.onIntent(SheikhListIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repository.sheikhCalls - callsBefore)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    private fun viewModel(repository: FakeAlmahirRepository) = SheikhListViewModel(
        getSheikhs = GetSheikhsUseCase(repository),
        observeBookmarks = ObserveBookmarksUseCase(repository),
        toggleBookmark = ToggleBookmarkUseCase(repository),
    )

    private companion object {
        private val AVAILABLE = Sheikh(
            id = "sheikh-1",
            name = "الشيخ أحمد",
            initials = "أح",
            avatarUrl = null,
            rating = 4.9,
            availability = SheikhAvailability.AVAILABLE,
        )
        private val BUSY = AVAILABLE.copy(
            id = "sheikh-2",
            name = "الشيخ عمر",
            availability = SheikhAvailability.IN_SESSION,
        )
    }
}
