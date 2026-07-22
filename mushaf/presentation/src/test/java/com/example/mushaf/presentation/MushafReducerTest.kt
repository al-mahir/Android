package com.example.mushaf.presentation

import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.ReaderPreferences
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationChunk
import com.example.mushaf.domain.model.recite.RecitationControl
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMatch
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.RecitationWordStatus
import com.example.mushaf.domain.repository.LiveRecitationRepository
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import com.example.mushaf.domain.repository.RecitationRepository
import com.example.mushaf.domain.usecase.StartLiveRecitationUseCase
import com.example.mushaf.domain.usecase.GetAyahTimingsUseCase
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.GetRecitersUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.presentation.audio.AudioPlayer
import com.example.mushaf.presentation.audio.AudioState
import com.example.mushaf.presentation.state.CaptureError
import com.example.mushaf.presentation.state.ChunkOutcome
import com.example.mushaf.presentation.state.MushafIntent
import com.iti.domain.core.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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

    private class FakeRecitationRepo : RecitationRepository {
        override fun getReciters(): Flow<Result<List<Reciter>>> = flowOf(Result.Success(emptyList()))
        override fun getTimingsForPage(reciterId: Int, pageNumber: Int): Flow<Result<List<AyahTiming>>> =
            flowOf(Result.Success(emptyList()))
    }

    private class FakeAudioPlayer : AudioPlayer {
        override val audioState = MutableStateFlow(AudioState.IDLE)
        override val currentPosition = MutableStateFlow(0L)
        override val currentTrackIndex = MutableStateFlow(0)
        override val playbackSpeed = MutableStateFlow(1f)
        override fun playUrls(urls: List<String>) = Unit
        override fun play() = Unit
        override fun pause() = Unit
        override fun stop() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun setSpeed(speed: Float) = Unit
        override fun release() = Unit
    }

    /**
     * Live-session stand-in.
     *
     * Scripts a session's event stream and records the controls it receives, so the reducer can
     * be driven through connect / grade / drop / finish without a microphone or a server.
     * [isRunning] proves the session is torn down on stop — a leaked session holds the mic.
     */
    private class FakeLiveRepo(
        private val script: List<LiveRecitationEvent> = emptyList(),
        private val failWith: Throwable? = null,
        /** Thrown once, then the session succeeds — models a dropped connection. */
        private val failFirstAttemptWith: Throwable? = null,
    ) : LiveRecitationRepository {
        var isRunning = false
            private set
        var sessionCount = 0
            private set
        var lastConfig: LiveRecitationConfig? = null
            private set
        val received = mutableListOf<RecitationControl>()

        override fun session(
            config: LiveRecitationConfig,
            controls: Flow<RecitationControl>,
        ): Flow<LiveRecitationEvent> = channelFlow {
            isRunning = true
            sessionCount++
            lastConfig = config
            try {
                failWith?.let { throw it }
                if (failFirstAttemptWith != null && sessionCount == 1) throw failFirstAttemptWith

                launch {
                    controls.collect { control ->
                        received += control
                        if (control is RecitationControl.Finish) {
                            send(LiveRecitationEvent.Finished)
                            close()
                        }
                    }
                }
                script.forEach { send(it) }
                awaitClose()
            } finally {
                isRunning = false
            }
        }
    }

    /** A graded chunk carrying one word with the given verdict. */
    private fun gradedChunk(
        wordIndex: Int = 0,
        status: RecitationWordStatus = RecitationWordStatus.ERROR,
        trimmed: Boolean = false,
        sequence: Int = 0,
        cursor: RecitationCursor? = RecitationCursor(1, 1, wordIndex),
    ) = LiveRecitationEvent.Graded(
        RecitationChunk(
            sequence = sequence,
            match = RecitationMatch.Matched(
                words = listOf(
                    RecitationWordFeedback(
                        position = RecitationCursor(1, 1, wordIndex),
                        uthmani = "بِسْمِ",
                        status = status,
                        mistakes = emptyList(),
                        isTrimmed = trimmed,
                    ),
                ),
                text = null,
                start = null,
                end = null,
            ),
            cursor = cursor,
            forcedCut = false,
            nonVerse = emptyList(),
        ),
    )

    private fun buildViewModel(
        prefs: FakePrefsRepo,
        mushafRepo: FakeMushafRepo = FakeMushafRepo(),
        liveRepo: FakeLiveRepo = FakeLiveRepo(),
    ): MushafViewModel {
        return MushafViewModel(
            getPage = GetPageUseCase(mushafRepo),
            observeReaderPreferences = ObserveReaderPreferencesUseCase(prefs),
            setTajweedEnabled = SetTajweedEnabledUseCase(prefs),
            saveLastPage = SaveLastPageUseCase(prefs),
            getReciters = GetRecitersUseCase(FakeRecitationRepo()),
            getAyahTimings = GetAyahTimingsUseCase(FakeRecitationRepo()),
            playbackManager = FakeAudioPlayer(),
            startLiveRecitation = StartLiveRecitationUseCase(liveRepo),
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

    // ===== Ta'ahud — live AI correction =====

    private fun recitingViewModel(liveRepo: FakeLiveRepo): MushafViewModel {
        val vm = buildViewModel(FakePrefsRepo(ReaderPreferences(lastPage = 1)), liveRepo = liveRepo)
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        return vm
    }

    private fun startedSession(vararg then: LiveRecitationEvent) = FakeLiveRepo(
        script = listOf(
            LiveRecitationEvent.Started(sessionId = "s1", engine = "real", requestedEngine = null),
        ) + then,
    )

    @Test
    fun `the mic starts a live session and stops it again`() = runTest(dispatcher) {
        val live = startedSession()
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()
        assertTrue(vm.state.value.isRecordingActive)
        assertTrue("session never started", live.isRunning)
        assertTrue(vm.state.value.liveCorrection.isActive)

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()
        assertFalse("session was not torn down", live.isRunning)
        assertFalse(vm.state.value.isRecordingActive)
    }

    @Test
    fun `stopping asks the server to flush rather than cancelling`() = runTest(dispatcher) {
        // Cancelling would drop the utterance still in flight — the last few seconds of what
        // was just recited, which is exactly the part the reciter is waiting to hear about.
        val live = startedSession()
        val vm = recitingViewModel(live)
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertTrue("no flush was requested", live.received.contains(RecitationControl.Finish))
    }

    @Test
    fun `graded words accumulate across chunks`() = runTest(dispatcher) {
        val live = startedSession(
            gradedChunk(wordIndex = 0, sequence = 0),
            gradedChunk(wordIndex = 1, sequence = 1),
        )
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        val correction = vm.state.value.liveCorrection
        assertEquals(setOf("1:1:1", "1:1:2"), correction.wordFeedback.keys)
        assertEquals(ChunkOutcome.GRADED, correction.lastOutcome)
        assertEquals(2, correction.mistakeCount)
    }

    @Test
    fun `a later trimmed report does not erase a scored verdict`() = runTest(dispatcher) {
        // API.md 5.4/5.5 show exactly this pair: a word scored in one chunk comes back trimmed
        // in the next. Overwriting would downgrade a verdict the reciter already earned.
        val live = startedSession(
            gradedChunk(wordIndex = 0, status = RecitationWordStatus.ERROR, trimmed = false, sequence = 0),
            gradedChunk(wordIndex = 0, status = RecitationWordStatus.CORRECT, trimmed = true, sequence = 1),
        )
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        val word = vm.state.value.liveCorrection.wordFeedback.getValue("1:1:1")
        assertFalse("an unscored report erased a real verdict", word.isTrimmed)
        assertTrue(word.countsAsMistake)
    }

    @Test
    fun `hints never reach the mistake count`() = runTest(dispatcher) {
        val live = startedSession(gradedChunk(status = RecitationWordStatus.ALMOST))
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.liveCorrection.wordFeedback.size)
        assertEquals("a softened finding was counted", 0, vm.state.value.liveCorrection.mistakeCount)
    }

    @Test
    fun `an ambiguous chunk surfaces its outcome and marks nothing`() = runTest(dispatcher) {
        val live = startedSession(
            LiveRecitationEvent.Graded(
                RecitationChunk(
                    sequence = 0,
                    match = RecitationMatch.Ambiguous(emptyList()),
                    cursor = null,
                    forcedCut = false,
                    nonVerse = emptyList(),
                ),
            ),
        )
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertEquals(ChunkOutcome.AMBIGUOUS, vm.state.value.liveCorrection.lastOutcome)
        assertTrue("an unplaced chunk marked words", vm.state.value.liveCorrection.wordFeedback.isEmpty())
    }

    @Test
    fun `an engine substitution is surfaced`() = runTest(dispatcher) {
        val live = FakeLiveRepo(
            script = listOf(
                LiveRecitationEvent.Started(sessionId = "s1", engine = "real", requestedEngine = "zipformer"),
            ),
        )
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()
        assertTrue(vm.state.value.liveCorrection.engineSubstituted)

        vm.onIntent(MushafIntent.DismissEngineNotice)
        advanceUntilIdle()
        assertFalse(vm.state.value.liveCorrection.engineSubstituted)
    }

    @Test
    fun `a dropped session reconnects from the last cursor`() = runTest(dispatcher) {
        // There is no server-side resumption: a reconnect opens a new session seeded with the
        // last cursor, costing only the in-flight chunk.
        val live = FakeLiveRepo(
            script = listOf(
                LiveRecitationEvent.Started(sessionId = "s2", engine = "real", requestedEngine = null),
            ),
            failFirstAttemptWith = IllegalStateException("socket dropped"),
        )
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertEquals("no reconnect was attempted", 2, live.sessionCount)
        assertTrue("session did not recover", vm.state.value.liveCorrection.isActive)
    }

    @Test
    fun `an unreachable service stops recording and says so`() = runTest(dispatcher) {
        // Silence would be read as a flawless recitation, so a failure has to be visible.
        val live = FakeLiveRepo(failWith = IllegalStateException("connection refused"))
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertFalse(vm.state.value.isRecordingActive)
        assertEquals(CaptureError.SERVICE_UNREACHABLE, vm.state.value.captureError)
    }

    @Test
    fun `a revoked permission stops recording without retrying`() = runTest(dispatcher) {
        val live = FakeLiveRepo(failWith = SecurityException("denied"))
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertFalse(vm.state.value.isRecordingActive)
        assertEquals(CaptureError.PERMISSION_DENIED, vm.state.value.captureError)
        assertEquals("a denied permission was retried", 1, live.sessionCount)
    }

    @Test
    fun `leaving the screen tears the session down`() = runTest(dispatcher) {
        val live = startedSession()
        val vm = recitingViewModel(live)
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        vm.onScreenStopped()
        advanceUntilIdle()

        assertFalse("session held after the screen stopped", live.isRunning)
        assertFalse(vm.state.value.isRecordingActive)
    }

    @Test
    fun `switching mode clears the session and its feedback`() = runTest(dispatcher) {
        val live = startedSession(gradedChunk())
        val vm = recitingViewModel(live)
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()
        assertTrue(vm.state.value.liveCorrection.wordFeedback.isNotEmpty())

        vm.onIntent(MushafIntent.SetMode(MushafMode.READING))
        advanceUntilIdle()

        assertFalse(live.isRunning)
        assertTrue("stale feedback survived a mode change", vm.state.value.liveCorrection.wordFeedback.isEmpty())
        assertFalse(vm.state.value.liveCorrection.isActive)
    }

    @Test
    fun `reading mode never opens a session`() = runTest(dispatcher) {
        val live = FakeLiveRepo()
        val vm = buildViewModel(FakePrefsRepo(ReaderPreferences(lastPage = 1)), liveRepo = live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertEquals(0, live.sessionCount)
        assertFalse(vm.state.value.isRecordingActive)
    }

    @Test
    fun `a page turn never invents a position to seek to`() = runTest(dispatcher) {
        // The fake pages carry no words, so no cursor can be derived. Seeking to a fabricated
        // position would itself make the tracker report mismatches that are not mistakes.
        val live = startedSession()
        val vm = recitingViewModel(live)
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.LoadPage(3))
        advanceUntilIdle()

        assertTrue(live.received.none { it is RecitationControl.Seek })
    }

    @Test
    fun `selecting a mistake opens and closes its detail`() = runTest(dispatcher) {
        val live = startedSession(gradedChunk())
        val vm = recitingViewModel(live)
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.SelectMistake("1:1:1"))
        advanceUntilIdle()
        assertEquals("1:1:1", vm.state.value.liveCorrection.selectedMistake?.wordId)

        vm.onIntent(MushafIntent.SelectMistake(null))
        advanceUntilIdle()
        assertEquals(null, vm.state.value.liveCorrection.selectedMistake)
    }
}
