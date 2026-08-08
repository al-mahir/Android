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
import com.example.mushaf.domain.model.recite.MoshafValue
import com.example.mushaf.domain.model.recite.RecitationStrictness
import com.example.mushaf.domain.model.recite.NonVerseSegment
import com.example.mushaf.domain.model.recite.RecitationCandidate
import com.example.mushaf.domain.model.recite.RecitationChunk
import com.example.mushaf.domain.model.recite.RecitationControl
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMatch
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.RecitationSettings
import com.example.mushaf.domain.model.recite.RecitationWordStatus
import com.example.mushaf.domain.repository.LiveRecitationRepository
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import com.example.mushaf.domain.repository.RecitationRepository
import com.example.mushaf.domain.repository.RecitationSettingsRepository
import com.example.mushaf.domain.usecase.ObserveRecitationSettingsUseCase
import com.example.mushaf.domain.usecase.StartLiveRecitationUseCase
import com.example.mushaf.domain.usecase.UpdateRecitationSettingsUseCase
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
import kotlinx.coroutines.flow.map
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
        override fun getPage(pageNumber: Int): Flow<Result<MushafPage>> {
            loadCounts[pageNumber] = (loadCounts[pageNumber] ?: 0) + 1
            return flowOf(Result.Success(MushafPage(pageNumber, emptyList())))
        }
        override suspend fun getPageCount(): Result<Int> = Result.Success(604)
        override suspend fun searchSurah(query: String): Result<List<com.example.mushaf.domain.model.Surah>> = Result.Success(emptyList())
        override suspend fun searchJuz(query: String): Result<List<com.example.mushaf.domain.model.Juz>> = Result.Success(emptyList())
        override suspend fun searchHizb(query: String): Result<List<com.example.mushaf.domain.model.Hizb>> = Result.Success(emptyList())
        override suspend fun searchPage(query: String): Result<List<Int>> = Result.Success(emptyList())
        override suspend fun searchAyah(query: String, limit: Int, offset: Int): Result<List<com.example.mushaf.domain.model.AyahSearchResult>> = Result.Success(emptyList())
        override suspend fun searchAyahByMeaning(query: String, mode: String, hyde: Boolean, limit: Int): Result<List<com.example.mushaf.domain.model.AyahSearchResult>> = Result.Success(emptyList())
        override suspend fun getSurahStartingPage(surahNumber: Int): Result<Int?> = Result.Success(null)
        override suspend fun getAyahPage(surahNumber: Int, ayahNumber: Int): Result<Int?> = Result.Success(null)
        override suspend fun getJuzStartingPage(juzNumber: Int): Result<Int?> = Result.Success(null)
        override suspend fun getAyahText(surahNumber: Int, ayahNumber: Int): Result<String?> = Result.Success(null)
        override suspend fun getTafsirForAyah(surah: Int, ayah: Int): Result<com.example.mushaf.domain.model.TafsirResult?> = Result.Success(null)
        override suspend fun getTafsirFromApi(surah: Int, ayah: Int, lang: String, tafsirKey: String): com.example.mushaf.domain.model.TafsirResult? = null
        override suspend fun getAvailableTafsirBooks(): List<com.example.mushaf.domain.model.TafsirBook> = emptyList()
        override fun observeAvailableTafsirBooks(): Flow<List<com.example.mushaf.domain.model.TafsirBook>> = kotlinx.coroutines.flow.emptyFlow()
        override suspend fun downloadTafsirBook(tafsirKey: String, downloadUrl: String) = Unit
        override fun deleteTafsirBook(tafsirKey: String) = Unit
        override suspend fun getTafsirFromLocalJson(tafsirKey: String, surah: Int, ayah: Int): com.example.mushaf.domain.model.TafsirResult? = null
        override suspend fun searchTafsir(query: String, limit: Int, offset: Int): Result<List<com.example.mushaf.domain.model.TafsirResult>> = Result.Success(emptyList())
    }

    private class FakePrefsRepo(initial: ReaderPreferences) : ReaderPreferencesRepository {
        val flow = MutableStateFlow(initial)
        var savedTajweed: Boolean? = null
        var savedPage: Int? = null
        override val preferences: Flow<ReaderPreferences> = flow
        override suspend fun setTajweedEnabled(enabled: Boolean): Result<Unit> {
            savedTajweed = enabled
            flow.value = flow.value.copy(tajweedEnabled = enabled)
            return Result.Success(Unit)
        }
        override suspend fun setLastPage(page: Int): Result<Unit> {
            savedPage = page
            flow.value = flow.value.copy(lastPage = page)
            return Result.Success(Unit)
        }
        override suspend fun setFirstMushafLaunchCompleted(): Result<Unit> = Result.Success(Unit)
        override suspend fun setDownloadOverWifiOnly(enabled: Boolean): Result<Unit> = Result.Success(Unit)
    }

    private class FakeRecitationRepo : RecitationRepository {
        override fun getReciters(): Flow<Result<List<Reciter>>> = flowOf(Result.Success(emptyList()))
        override fun getTimingsForPage(reciterId: Int, pageNumber: Int): Flow<Result<List<AyahTiming>>> =
            flowOf(Result.Success(emptyList()))
        override suspend fun downloadRecitation(reciterId: Int, surahNumber: Int?) = Unit
        override fun cancelDownloadRecitation(reciterId: Int, surahNumber: Int?) = Unit
        override fun observeDownloadProgress(reciterId: Int): Flow<List<com.example.mushaf.domain.model.DownloadStatus>> = flowOf(emptyList())
    }

    private class FakeAudioPlayer : AudioPlayer {
        override val audioState = MutableStateFlow(AudioState.IDLE)
        override val currentPosition = MutableStateFlow(0L)
        override val currentTrackIndex = MutableStateFlow(0)
        override val playbackSpeed = MutableStateFlow(1f)
        override val externalCommands = kotlinx.coroutines.flow.MutableSharedFlow<String>()
        override fun playTracks(tracks: List<AudioPlayer.AudioTrackInfo>, startIndex: Int, autoPlay: Boolean) = Unit
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
        ): Flow<Result<LiveRecitationEvent>> = channelFlow<LiveRecitationEvent> {
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
        }.map { Result.Success(it) }
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
        override fun observeSessions(): Flow<Result<List<RecitationSessionSummary>>> = flowOf(Result.Success(saved))
        override fun observeSession(id: String): Flow<Result<RecitationSessionSummary?>> =
            flowOf(Result.Success(saved.firstOrNull { it.id == id }))
        override suspend fun save(summary: RecitationSessionSummary): Result<Unit> {
            saved += summary
            return Result.Success(Unit)
        }
        override suspend fun delete(id: String): Result<Unit> {
            saved.removeAll { it.id == id }
            return Result.Success(Unit)
        }
        override suspend fun deleteAll(): Result<Unit> {
            saved.clear()
            return Result.Success(Unit)
        }
    }


    private class FakeSettingsRepo(
        initial: RecitationSettings = RecitationSettings(),
    ) : RecitationSettingsRepository {
        private val state = MutableStateFlow(initial)
        override val settings: Flow<RecitationSettings> = state
        override suspend fun update(settings: RecitationSettings): Result<Unit> {
            state.value = settings
            return Result.Success(Unit)
        }
        val current: RecitationSettings get() = state.value
    }

    private class FakeAppPreferencesRepo : com.iti.domain.settings.repository.AppPreferencesRepository {
        override val preferences = MutableStateFlow(com.iti.domain.settings.model.AppPreferences())
        override suspend fun setThemeMode(mode: com.iti.domain.settings.model.ThemeMode) = Result.Success(Unit)
        override suspend fun setLanguage(language: com.iti.domain.settings.model.AppLanguage) = Result.Success(Unit)
        override suspend fun setRemindersEnabled(enabled: Boolean) = Result.Success(Unit)
        override suspend fun setErrorSoundsEnabled(enabled: Boolean) = Result.Success(Unit)
        override suspend fun setDataSaverEnabled(enabled: Boolean) = Result.Success(Unit)
        override suspend fun saveUser(user: com.iti.domain.model.User) = Result.Success(Unit)
        override suspend fun clearUser() = Result.Success(Unit)
    }

    private class FakeConnectivityObserver : com.iti.domain.connectivity.ConnectivityObserver {
        override val status = MutableStateFlow(com.iti.domain.connectivity.ConnectivityStatus.Available)
        override fun currentStatus() = com.iti.domain.connectivity.ConnectivityStatus.Available
    }

    private class FakeAlmahirRepository : com.iti.domain.repository.AlmahirRepository {
        private val bookmarks = mutableMapOf<String, com.iti.domain.model.Bookmark>()
        override fun observeCurrentUser() = flowOf<Result<com.iti.domain.model.User>>()
        override fun observeSubscription() = flowOf<Result<com.iti.domain.model.Subscription>>()
        override fun observeSubscriptionPackages() =
            flowOf<Result<List<com.iti.domain.model.SubscriptionPackage>>>()
        override suspend fun startFreeTrial(): Result<com.iti.domain.model.Subscription> =
            Result.Success(com.iti.domain.model.Subscription(com.iti.domain.model.SubscriptionPlan.NONE, null))
        override suspend fun selectSubscriptionPackage(packageId: String): Result<com.iti.domain.model.Subscription> =
            Result.Success(com.iti.domain.model.Subscription(com.iti.domain.model.SubscriptionPlan.NONE, null))
        override fun observeLegalDocument(type: com.iti.domain.model.LegalDocumentType) = flowOf<Result<com.iti.domain.model.LegalDocument>>()
        override suspend fun requestSubscriptionCancellation(message: String) = Result.Success(Unit)
        override suspend fun logout() = Result.Success(Unit)
        override suspend fun deleteAccount() = Result.Success(Unit)
        override fun observeBookmarks(type: com.iti.domain.model.BookmarkType) =
            flowOf(Result.Success(bookmarks.values.filter { it.type == type }))
        override fun observeAllBookmarks() = flowOf(Result.Success(bookmarks.values.toList()))
        override suspend fun getBookmarks(type: com.iti.domain.model.BookmarkType) =
            Result.Success(bookmarks.values.filter { it.type == type })
        override suspend fun getBookmark(id: String) = Result.Success(bookmarks[id])
        override suspend fun addBookmark(bookmark: com.iti.domain.model.Bookmark): Result<Unit> {
            bookmarks[bookmark.id] = bookmark
            return Result.Success(Unit)
        }
        override suspend fun removeBookmark(id: String): Result<Unit> {
            bookmarks.remove(id)
            return Result.Success(Unit)
        }
        override fun observeMeetingStatuses(userId: String) =
            flowOf<Result<List<com.iti.domain.model.MeetingStatus>>>(Result.Success(emptyList()))
        override suspend fun saveMeetingStatus(status: com.iti.domain.model.MeetingStatus) = Result.Success(Unit)
    }

    private fun buildViewModel(
        prefs: FakePrefsRepo,
        mushafRepo: MushafRepository = FakeMushafRepo(),
        recitationRepo: RecitationRepository = FakeRecitationRepo(),
        liveRepo: FakeLiveRepo = FakeLiveRepo(),
        sessionRepo: FakeSessionRepo = FakeSessionRepo(),
        settingsRepo: FakeSettingsRepo = FakeSettingsRepo(),
        appPrefsRepo: com.iti.domain.settings.repository.AppPreferencesRepository = FakeAppPreferencesRepo(),
        connectivityObserver: com.iti.domain.connectivity.ConnectivityObserver = FakeConnectivityObserver(),
        almahirRepository: FakeAlmahirRepository = FakeAlmahirRepository(),
    ): MushafViewModel {
        return MushafViewModel(
            getPage = GetPageUseCase(mushafRepo),
            observeReaderPreferences = ObserveReaderPreferencesUseCase(prefs),
            setTajweedEnabled = SetTajweedEnabledUseCase(prefs),
            setFirstMushafLaunchCompleted = com.example.mushaf.domain.usecase.SetFirstMushafLaunchCompletedUseCase(prefs),
            saveLastPage = SaveLastPageUseCase(prefs),
            getReciters = GetRecitersUseCase(recitationRepo),
            getAyahTimings = GetAyahTimingsUseCase(recitationRepo),
            getTafsirForAyah = com.example.mushaf.domain.usecase.GetTafsirForAyahUseCase(mushafRepo),
            playbackManager = FakeAudioPlayer(),
            startLiveRecitation = StartLiveRecitationUseCase(liveRepo),
            saveRecitationSession = SaveRecitationSessionUseCase(sessionRepo),
            observeRecitationSettings = ObserveRecitationSettingsUseCase(settingsRepo),
            updateRecitationSettings = UpdateRecitationSettingsUseCase(settingsRepo),
            downloadRecitation = com.example.mushaf.domain.usecase.DownloadRecitationUseCase(recitationRepo),
            observeAvailableTafsirBooks = com.example.mushaf.domain.usecase.ObserveAvailableTafsirBooksUseCase(mushafRepo),
            manageTafsirDownload = com.example.mushaf.domain.usecase.ManageTafsirDownloadUseCase(mushafRepo),
            observeAppPreferences = com.iti.domain.usecase.settings.ObserveAppPreferencesUseCase(appPrefsRepo),
            connectivityObserver = connectivityObserver,
            toggleBookmarkUseCase = com.iti.domain.usecase.bookmark.ToggleBookmarkUseCase(almahirRepository),
            observeBookmarks = com.iti.domain.usecase.bookmark.ObserveBookmarksUseCase(almahirRepository),
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

    @Test
    fun `speech level events never move the highlight without a confirmed word`() = runTest(dispatcher) {
        val live = startedSession(
            LiveRecitationEvent.Level(amplitude = 0.6f, isSpeaking = true),
            LiveRecitationEvent.Level(amplitude = 0.6f, isSpeaking = true),
            LiveRecitationEvent.Level(amplitude = 0f, isSpeaking = false),
        )
        val vm = recitingViewModel(live)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertNull(
            "a timing guess moved the highlight without any confirmed word",
            vm.state.value.highlightedWordId,
        )
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
    fun `a session carries the stored tuning rather than a hardcoded engine`() = runTest(dispatcher) {
        val live = startedSession()
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 1)),
            liveRepo = live,
            settingsRepo = FakeSettingsRepo(
                RecitationSettings(
                    engine = "real",
                    strictness = RecitationStrictness.STRICT,
                    gradedRules = setOf("aared_madd"),
                    moshaf = mapOf("madd_monfasel_len" to MoshafValue.Number(4)),
                ),
            ),
        )
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        val config = live.lastConfig
        assertEquals("real", config?.engine)
        assertEquals(RecitationStrictness.STRICT, config?.strictness)
        assertEquals(setOf("aared_madd"), config?.gradedRules)
        assertEquals(mapOf("madd_monfasel_len" to MoshafValue.Number(4)), config?.moshaf)
    }


    @Test
    fun `the session starts immediately at the top of the page`() = runTest(dispatcher) {
        val live = startedSession()
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 5)),
            mushafRepo = WordedMushafRepo(),
            liveRepo = live,
        )
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        advanceUntilIdle()

        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        assertEquals(RecitationCursor.fromWordId("5:1:1"), live.lastConfig?.start)
    }

    @Test
    fun `hifz-only grading sends an empty rule list, not a null one`() = runTest(dispatcher) {
        val live = startedSession()
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 1)),
            liveRepo = live,
            settingsRepo = FakeSettingsRepo(
                RecitationSettings(tajweedGradingEnabled = false, gradedRules = setOf("ghonna")),
            ),
        )
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        
        
        assertEquals(emptySet<String>(), live.lastConfig?.gradedRules)
    }

    @Test
    fun `switching grading mode mid-session reopens it at the current cursor`() = runTest(dispatcher) {
        val sessions = FakeSessionRepo()
        val live = FakeLiveRepo(
            scripts = listOf(
                listOf(
                    LiveRecitationEvent.Started("s1", engine = "real", requestedEngine = null),
                    gradedChunk(cursor = RecitationCursor(2, 5, 3)),
                ),
                listOf(LiveRecitationEvent.Started("s2", engine = "real", requestedEngine = null)),
            ),
        )
        val settings = FakeSettingsRepo()
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 1)),
            liveRepo = live,
            sessionRepo = sessions,
            settingsRepo = settings,
        )
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        advanceUntilIdle()
        vm.onIntent(MushafIntent.ToggleRecording)
        advanceUntilIdle()

        vm.onIntent(MushafIntent.SetTajweedGrading(enabled = false))
        advanceUntilIdle()

        assertEquals("the session was not reopened", 2, live.sessionCount)
        assertEquals("the new setting did not reach the wire", emptySet<String>(), live.lastConfig?.gradedRules)
        assertEquals(
            "the reciter was sent back to the top of the page",
            RecitationCursor(2, 5, 3),
            live.lastConfig?.start,
        )
        assertTrue("recording stopped instead of continuing", vm.state.value.isRecordingActive)
        assertFalse(settings.current.tajweedGradingEnabled)

        
        
        assertEquals("the recited minutes were discarded", 1, sessions.saved.size)
        assertNull("a summary interrupted the reciter", vm.state.value.sessionSummary)
    }

    @Test
    fun `switching grading mode outside a session only stores it`() = runTest(dispatcher) {
        val live = FakeLiveRepo()
        val settings = FakeSettingsRepo()
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 1)),
            liveRepo = live,
            settingsRepo = settings,
        )
        vm.onIntent(MushafIntent.SetMode(MushafMode.RECITATION))
        advanceUntilIdle()

        vm.onIntent(MushafIntent.SetTajweedGrading(enabled = false))
        advanceUntilIdle()

        assertEquals("a session was started by a settings change", 0, live.sessionCount)
        assertFalse(settings.current.tajweedGradingEnabled)
        assertFalse(vm.state.value.isTajweedGradingEnabled)
    }

    @Test
    fun `a follow-along engine disables the grading toggle`() = runTest(dispatcher) {
        val vm = buildViewModel(
            FakePrefsRepo(ReaderPreferences(lastPage = 1)),
            settingsRepo = FakeSettingsRepo(RecitationSettings(engine = "zipformer")),
        )
        advanceUntilIdle()

        
        
        assertFalse(vm.state.value.canGradeTajweed)
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
        override fun getPage(pageNumber: Int): Flow<Result<MushafPage>> = flowOf(
            Result.Success(
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
            ),
        )

        override suspend fun getPageCount(): Result<Int> = Result.Success(604)
        override suspend fun searchSurah(query: String): Result<List<com.example.mushaf.domain.model.Surah>> = Result.Success(emptyList())
        override suspend fun searchJuz(query: String): Result<List<com.example.mushaf.domain.model.Juz>> = Result.Success(emptyList())
        override suspend fun searchHizb(query: String): Result<List<com.example.mushaf.domain.model.Hizb>> = Result.Success(emptyList())
        override suspend fun searchPage(query: String): Result<List<Int>> = Result.Success(emptyList())
        override suspend fun searchAyah(query: String, limit: Int, offset: Int): Result<List<com.example.mushaf.domain.model.AyahSearchResult>> = Result.Success(emptyList())
        override suspend fun searchAyahByMeaning(query: String, mode: String, hyde: Boolean, limit: Int): Result<List<com.example.mushaf.domain.model.AyahSearchResult>> = Result.Success(emptyList())
        override suspend fun getSurahStartingPage(surahNumber: Int): Result<Int?> = Result.Success(null)
        override suspend fun getAyahPage(surahNumber: Int, ayahNumber: Int): Result<Int?> = Result.Success(null)
        override suspend fun getJuzStartingPage(juzNumber: Int): Result<Int?> = Result.Success(null)
        override suspend fun getAyahText(surahNumber: Int, ayahNumber: Int): Result<String?> = Result.Success(null)
        override suspend fun getTafsirForAyah(surah: Int, ayah: Int): Result<com.example.mushaf.domain.model.TafsirResult?> = Result.Success(null)
        override suspend fun getTafsirFromApi(surah: Int, ayah: Int, lang: String, tafsirKey: String): com.example.mushaf.domain.model.TafsirResult? = null
        override suspend fun getAvailableTafsirBooks(): List<com.example.mushaf.domain.model.TafsirBook> = emptyList()
        override fun observeAvailableTafsirBooks(): Flow<List<com.example.mushaf.domain.model.TafsirBook>> = kotlinx.coroutines.flow.emptyFlow()
        override suspend fun downloadTafsirBook(tafsirKey: String, downloadUrl: String) = Unit
        override fun deleteTafsirBook(tafsirKey: String) = Unit
        override suspend fun getTafsirFromLocalJson(tafsirKey: String, surah: Int, ayah: Int): com.example.mushaf.domain.model.TafsirResult? = null
        override suspend fun searchTafsir(query: String, limit: Int, offset: Int): Result<List<com.example.mushaf.domain.model.TafsirResult>> = Result.Success(emptyList())
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
