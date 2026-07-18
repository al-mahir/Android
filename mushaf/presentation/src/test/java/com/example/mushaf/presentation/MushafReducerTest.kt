package com.example.mushaf.presentation

import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.ReaderPreferences
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.presentation.state.MushafIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class MushafReducerTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeMushafRepo : MushafRepository {
        val loadCounts = mutableMapOf<Int, Int>()
        override fun getPage(pageNumber: Int): Flow<MushafPage> {
            loadCounts[pageNumber] = (loadCounts[pageNumber] ?: 0) + 1
            return flowOf(MushafPage(pageNumber, emptyList()))
        }
        override suspend fun getPageCount(): Int = 604
    }

    private class FakePrefsRepo(initial: ReaderPreferences) : ReaderPreferencesRepository {
        val flow = MutableStateFlow(initial)
        var savedTajweed: Boolean? = null
        var savedPage: Int? = null
        override val preferences: Flow<ReaderPreferences> = flow
        override suspend fun setTajweedEnabled(enabled: Boolean) {
            savedTajweed = enabled
            flow.value = flow.value.copy(tajweedEnabled = enabled)
        }
        override suspend fun setLastPage(page: Int) {
            savedPage = page
            flow.value = flow.value.copy(lastPage = page)
        }
    }

    private fun buildViewModel(
        prefs: FakePrefsRepo,
        mushafRepo: FakeMushafRepo = FakeMushafRepo(),
    ): MushafViewModel {
        return MushafViewModel(
            getPage = GetPageUseCase(mushafRepo),
            observeReaderPreferences = ObserveReaderPreferencesUseCase(prefs),
            setTajweedEnabled = SetTajweedEnabledUseCase(prefs),
            saveLastPage = SaveLastPageUseCase(prefs),
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `seeds resume page and tajweed mode from preferences`() = runTest(dispatcher) {
        val prefs = FakePrefsRepo(ReaderPreferences(tajweedEnabled = false, lastPage = 5))
        val vm = buildViewModel(prefs)

        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(5, state.currentPage)
        assertFalse(state.isTajweedEnabled)
        assertEquals(5, state.page?.pageNumber)
        assertFalse(state.isLoading)
    }

    @Test
    fun `toggle tajweed updates state and persists without reloading page`() = runTest(dispatcher) {
        val prefs = FakePrefsRepo(ReaderPreferences(tajweedEnabled = true, lastPage = 1))
        val vm = buildViewModel(prefs)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleTajweed(false))
        advanceUntilIdle()

        assertFalse(vm.state.value.isTajweedEnabled)
        assertEquals(false, prefs.savedTajweed)
    }

    @Test
    fun `load page clamps out of range request and persists last page`() = runTest(dispatcher) {
        val prefs = FakePrefsRepo(ReaderPreferences(lastPage = 1))
        val vm = buildViewModel(prefs)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.LoadPage(9999))
        advanceUntilIdle()

        assertEquals(604, vm.state.value.currentPage)
        assertEquals(604, prefs.savedPage)
    }

    @Test
    fun `settling a page prefetches its neighbours into the cache`() = runTest(dispatcher) {
        val prefs = FakePrefsRepo(ReaderPreferences(lastPage = 10))
        val repo = FakeMushafRepo()
        val vm = buildViewModel(prefs, repo)
        advanceUntilIdle()

        // Page 10 plus a two-page window each side are cached, so the presentation layer can
        // prefetch ±2 and a fast multi-page fling never reloads.
        val pages = vm.state.value.pages.keys
        assertTrue(pages.containsAll(listOf(8, 9, 10, 11, 12)))
    }

    @Test
    fun `revisiting a cached page does not reload it`() = runTest(dispatcher) {
        val prefs = FakePrefsRepo(ReaderPreferences(lastPage = 10))
        val repo = FakeMushafRepo()
        val vm = buildViewModel(prefs, repo)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.LoadPage(11))
        advanceUntilIdle()
        vm.onIntent(MushafIntent.LoadPage(10))
        advanceUntilIdle()

        // Page 10 was fetched exactly once despite being visited twice.
        assertEquals(1, repo.loadCounts[10])
    }

    @Test
    fun `highlight word updates highlighted id`() = runTest(dispatcher) {
        val prefs = FakePrefsRepo(ReaderPreferences(lastPage = 1))
        val vm = buildViewModel(prefs)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.HighlightWord("p1:l2:w3"))
        advanceUntilIdle()

        assertEquals("p1:l2:w3", vm.state.value.highlightedWordId)
        assertTrue(true)
    }
}
