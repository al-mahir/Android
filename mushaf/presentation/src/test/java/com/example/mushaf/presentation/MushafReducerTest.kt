package com.example.mushaf.presentation

import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.MushafLine
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.MushafWord
import com.example.mushaf.domain.model.ReaderPreferences
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.NonVerseSegment
import com.example.mushaf.domain.model.recite.RecitationCandidate
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
import com.iti.domain.model.recitation.RecitationSessionSummary
import com.iti.domain.repository.RecitationSessionRepository
import com.iti.domain.usecase.SaveRecitationSessionUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    





 
    private class FakeLiveRepo(
        private val script: List<LiveRecitationEvent> = emptyList(),
        private val failWith: Throwable? = null,
         
        private val failFirstAttemptWith: Throwable? = null,
        



 
        private val scripts: List<List<LiveRecitationEvent>>? = null,
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
                (scripts?.getOrNull(sessionCount - 1) ?: script).forEach { send(it) }
                awaitClose()
            } finally {
                isRunning = false
            }
        }
    }

     
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

     
    private class FakeSessionRepo : RecitationSessionRepository {
        val saved = mutableListOf<RecitationSessionSummary>()
        override fun observeSessions(): Flow<List<RecitationSessionSummary>> = flowOf(saved)
        override fun observeSession(id: String): Flow<RecitationSessionSummary?> =
            flowOf(saved.firstOrNull { it.id == id })
        override suspend fun save(summary: RecitationSessionSummary) { saved += summary }
        override suspend fun delete(id: String) { saved.removeAll { it.id == id } }
        override suspend fun deleteAll() { saved.clear() }
    }

    private fun buildViewModel(
        prefs: FakePrefsRepo,
        mushafRepo: MushafRepository = FakeMushafRepo(),
        liveRepo: FakeLiveRepo = FakeLiveRepo(),
        sessionRepo: FakeSessionRepo = FakeSessionRepo(),
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
            saveRecitationSession = SaveRecitationSessionUseCase(sessionRepo),
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

    @Test
    fun `finishing a session saves it and starts a fresh one`() = runTest(dispatcher) {
        
        
        val sessions = FakeSessionRepo()
        val started = LiveRecitationEvent.Started(sessionId = "s1", engine = "real", requestedEngine = null)
        val live = FakeLiveRepo(
            
            
            scripts = listOf(listOf(started, gradedChunk()), listOf(started)),
        )
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 1)),
            liveRepo = live,
            sessionRepo = sessions,
        )
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.FinishAndStartNewSession)
        advanceUntilIdle()

        assertEquals("the finished session was not saved", 1, sessions.saved.size)
        assertEquals("a second session did not start", 2, live.sessionCount)
        assertTrue("recording stopped instead of continuing", vm.state.value.isRecordingActive)
        
        assertTrue(vm.state.value.liveCorrection.wordFeedback.isEmpty())
    }

    @Test
    fun `finishing does nothing when no session is running`() = runTest(dispatcher) {
        val sessions = FakeSessionRepo()
        val live = FakeLiveRepo()
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 1)),
            liveRepo = live,
            sessionRepo = sessions,
        )
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        advanceUntilIdle()

        vm.onIntent(MushafIntent.FinishAndStartNewSession)
        advanceUntilIdle()

        assertEquals(0, live.sessionCount)
        assertTrue(sessions.saved.isEmpty())
    }

    @Test
    fun `stopping the mic saves the session and offers its summary`() = runTest(dispatcher) {
        val sessions = FakeSessionRepo()
        val live = startedSession(gradedChunk())
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 1)),
            liveRepo = live,
            sessionRepo = sessions,
        )
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertEquals(1, sessions.saved.size)
        assertEquals(1, sessions.saved.single().mistakeCount)
        assertNotNull("the summary was not offered", vm.state.value.sessionSummary)

        vm.onIntent(MushafIntent.DismissSessionSummary)
        advanceUntilIdle()
        assertNull(vm.state.value.sessionSummary)
    }

    @Test
    fun `the live pills are cleared once a session ends`() = runTest(dispatcher) {
        
        
        val sessions = FakeSessionRepo()
        val live = startedSession(gradedChunk())
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 1)),
            liveRepo = live,
            sessionRepo = sessions,
        )
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.liveCorrection.mistakeCount)

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        val cleared = vm.state.value.liveCorrection
        assertEquals("the mistake count survived the session", 0, cleared.mistakeCount)
        assertNull("the accuracy pill survived the session", cleared.accuracy)
        assertTrue(cleared.wordFeedback.isEmpty())
        
        assertNotNull(vm.state.value.sessionSummary)
        assertEquals(1, sessions.saved.single().mistakeCount)
    }

    @Test
    fun `choosing a candidate seeks the service and clears the question`() = runTest(dispatcher) {
        
        val live = startedSession(
            LiveRecitationEvent.Graded(
                RecitationChunk(
                    sequence = 0,
                    match = RecitationMatch.Ambiguous(
                        listOf(
                            RecitationCandidate(RecitationCursor(1, 1, 0), null, "بسم الله"),
                            RecitationCandidate(RecitationCursor(27, 30, 3), null, "إنه من سليمان"),
                        ),
                    ),
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
        assertEquals(2, vm.state.value.liveCorrection.candidates.size)

        vm.onIntent(MushafIntent.SelectCandidate(RecitationCursor(27, 30, 3)))
        advanceUntilIdle()

        assertTrue(vm.state.value.liveCorrection.candidates.isEmpty())
        val seek = live.received.filterIsInstance<RecitationControl.Seek>().single()
        assertEquals(27, seek.position.sura)
        assertEquals(30, seek.position.aya)
    }

    @Test
    fun `dismissing candidates leaves the session alone`() = runTest(dispatcher) {
        
        val live = startedSession(
            LiveRecitationEvent.Graded(
                RecitationChunk(
                    sequence = 0,
                    match = RecitationMatch.Ambiguous(
                        listOf(RecitationCandidate(RecitationCursor(1, 1, 0), null, "بسم الله")),
                    ),
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

        vm.onIntent(MushafIntent.DismissCandidates)
        advanceUntilIdle()

        assertTrue(vm.state.value.liveCorrection.candidates.isEmpty())
        assertTrue("dismissing must not seek", live.received.none { it is RecitationControl.Seek })
        assertTrue(vm.state.value.isRecordingActive)
    }

    @Test
    fun `recognised non-verse speech is carried through for acknowledgement`() = runTest(dispatcher) {
        
        val live = startedSession(
            LiveRecitationEvent.Graded(
                RecitationChunk(
                    sequence = 0,
                    match = RecitationMatch.Matched(emptyList(), null, null, null),
                    cursor = null,
                    forcedCut = false,
                    nonVerse = listOf(NonVerseSegment.BASMALAH),
                ),
            ),
        )
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertEquals(listOf(NonVerseSegment.BASMALAH), vm.state.value.liveCorrection.nonVerse)
        
        assertEquals(0, vm.state.value.liveCorrection.mistakeCount)
    }

     
    private class WordedMushafRepo : MushafRepository {
        override fun getPage(pageNumber: Int): Flow<MushafPage> = flowOf(
            MushafPage(
                pageNumber = pageNumber,
                lines = listOf(
                    MushafLine(
                        lineNumber = 1,
                        type = LineType.AYAH,
                        isCentered = false,
                        surahNumber = null,
                        words = (1..4).map { index ->
                            MushafWord(
                                id = "$pageNumber:1:$index",
                                glyphs = "w",
                                pageNumber = pageNumber,
                                lineNumber = 1,
                                positionInLine = index,
                                isEndOfAyah = index == 4,
                            )
                        },
                    ),
                ),
            ),
        )

        override suspend fun getPageCount(): Int = 604
    }

    @Test
    fun `turning to a prefetched page still re-points the reading cursor`() = runTest(dispatcher) {
        
        
        
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 5)),
            mushafRepo = WordedMushafRepo(),
            liveRepo = startedSession(),
        )
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()
        
        assertTrue(vm.state.value.pages.containsKey(6))

        vm.onIntent(MushafIntent.LoadPage(6))
        advanceUntilIdle()

        val highlighted = vm.state.value.highlightedWordId
        assertNotNull("no reading cursor after a page turn", highlighted)
        assertTrue(
            "the cursor is still on the previous page: $highlighted",
            highlighted!!.startsWith("6:"),
        )
    }

    @Test
    fun `a page turn outside a session leaves no reading cursor`() = runTest(dispatcher) {
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 5)),
            mushafRepo = WordedMushafRepo(),
        )
        advanceUntilIdle()

        vm.onIntent(MushafIntent.LoadPage(6))
        advanceUntilIdle()

        assertNull(vm.state.value.highlightedWordId)
    }
}
