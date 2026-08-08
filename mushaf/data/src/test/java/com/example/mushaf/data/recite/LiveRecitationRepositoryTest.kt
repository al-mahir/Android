package com.example.mushaf.data.recite

import com.example.mushaf.data.recite.remote.AiServiceConfig
import com.example.mushaf.data.recite.remote.LiveRecitationSocket
import com.example.mushaf.data.repository.LiveRecitationRepositoryImpl
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import com.example.mushaf.domain.model.recite.RecitationControl
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMatch
import com.example.mushaf.domain.model.recite.SpeechEvent
import com.example.mushaf.domain.model.recite.SpeechGateConfig
import com.example.mushaf.domain.model.recite.local.AsrModelState
import com.example.mushaf.domain.repository.AsrModelRepository
import com.example.mushaf.domain.repository.LocalSpeechRecognizer
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import com.iti.domain.core.Result
import com.iti.domain.core.getOrNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class LiveRecitationRepositoryTest {

    private lateinit var service: FakeAiService
    private lateinit var client: HttpClient

    @Before
    fun setUp() {
        client = HttpClient(OkHttp) { install(WebSockets) }
    }

    @After
    fun tearDown() {
        client.close()
        if (::service.isInitialized) service.stop()
    }

    private class FakeCapture(
        private val frameCount: Int,
        private val isSpeech: Boolean = true,
    ) : RecitationCaptureRepository {
        val isCapturing = AtomicBoolean(false)

        @Volatile
        var releasedAt: Long = 0

        override fun capture(): Flow<Result<AudioFrame>> = throw UnsupportedOperationException()

        override fun captureSpeech(config: SpeechGateConfig): Flow<Result<SpeechEvent>> = flow {
            isCapturing.set(true)
            try {
                repeat(frameCount) { index ->
                    emit(
                        SpeechEvent.Audio(
                            AudioFrame(
                                ShortArray(RecitationAudioFormat.FRAME_SAMPLES) { 4_000 },
                                startSample = index.toLong() * RecitationAudioFormat.FRAME_SAMPLES,
                            ),
                            isSpeech = isSpeech,
                        ),
                    )
                }
                emit(SpeechEvent.SpeechEnded)
                kotlinx.coroutines.awaitCancellation()
            } finally {
                isCapturing.set(false)
                releasedAt = System.nanoTime()
            }
        }.map { Result.Success(it) }
    }

    /** Emits [words] once accept() has been called at least once, then stays open - mirrors a
     * real streaming recognizer, which never completes on its own. */
    private class FakeLocalSpeechRecognizer(
        override val isAvailable: Boolean = true,
        private val scriptedWords: List<String> = emptyList(),
        private val failure: Throwable? = null,
    ) : LocalSpeechRecognizer {
        private val _words = MutableSharedFlow<String>(extraBufferCapacity = 64)
        override val words: SharedFlow<String> = _words.asSharedFlow()
        private var emitted = false

        override suspend fun accept(frame: com.example.mushaf.domain.model.recite.AudioFrame) {
            failure?.let { throw it }
            if (emitted) return
            emitted = true
            scriptedWords.forEach { _words.emit(it) }
        }

        override fun reset() {
            emitted = false
        }
    }

    private class FakeAsrModelRepository(
        initial: AsrModelState = AsrModelState.Ready,
    ) : AsrModelRepository {
        override val state: StateFlow<AsrModelState> = MutableStateFlow(initial)
        override fun ensureAvailable() = Unit
    }

    private fun repositoryFor(
        capture: RecitationCaptureRepository,
        localSpeechRecognizer: LocalSpeechRecognizer = FakeLocalSpeechRecognizer(),
    ) = LiveRecitationRepositoryImpl(
        capture = capture,
        socket = LiveRecitationSocket(client, AiServiceConfig(authority = "localhost:${service.port}", secure = false)),
        localSpeechRecognizer = localSpeechRecognizer,
        asrModelRepository = FakeAsrModelRepository(),
    )

    private suspend fun runSession(
        capture: RecitationCaptureRepository,
        config: LiveRecitationConfig = LiveRecitationConfig(start = RecitationCursor(1, 1)),
        localSpeechRecognizer: LocalSpeechRecognizer = FakeLocalSpeechRecognizer(),
        beforeFinish: suspend (MutableSharedFlow<RecitationControl>) -> Unit = {},
    ): List<LiveRecitationEvent> = withTimeout(TIMEOUT_MS) {
        val controls = MutableSharedFlow<RecitationControl>()
        val events = mutableListOf<LiveRecitationEvent>()
        coroutineScope {
            val session = launch {
                repositoryFor(capture, localSpeechRecognizer).session(config, controls).map { it.getOrNull()!! }.toList(events)
            }
            awaitUntil("session started") { events.any { it is LiveRecitationEvent.Started } }
            beforeFinish(controls)
            controls.emit(RecitationControl.Finish)
            session.join()
        }
        events
    }

    private suspend fun awaitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + AWAIT_MS
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            delay(10)
        }
        throw AssertionError("timed out waiting for: $what")
    }

    @Test
    fun `a session streams captured speech and finishes on the server's done`() = runBlocking {
        service = FakeAiService(feedbackPayloads = listOf(FATIHA_FEEDBACK_JSON)).start()
        val capture = FakeCapture(frameCount = 5)

        val events = runSession(capture) {
            awaitUntil("all audio streamed") { service.binaryFrameCount.get() == 5 }
        }

        // Started arrives first: the microphone is not opened until the handshake succeeds.
        assertTrue(events.first() is LiveRecitationEvent.Started)
        assertTrue("session never finished", events.last() is LiveRecitationEvent.Finished)
        assertEquals(5, service.binaryFrameCount.get())
    }

    @Test
    fun `the microphone is released before end is sent`() = runBlocking {
        // Ordering matters: audio arriving after the flush request is discarded by the server,
        // so the reciter's last words would vanish rather than be graded.
        service = FakeAiService().start()
        val capture = FakeCapture(frameCount = 3)

        runSession(capture) {
            awaitUntil("audio streamed") { service.binaryFrameCount.get() == 3 }
        }

        assertTrue("end was never sent", service.receivedEnd)
        assertFalse("microphone still held after the session", capture.isCapturing.get())
        assertTrue("microphone was never opened", capture.releasedAt > 0)
    }

    @Test
    fun `graded chunks arrive as domain models with their word ids`() = runBlocking {
        service = FakeAiService(feedbackPayloads = listOf(FATIHA_FEEDBACK_JSON)).start()

        val events = runSession(FakeCapture(frameCount = 2))

        val chunk = events.filterIsInstance<LiveRecitationEvent.Graded>().single().chunk
        assertTrue(chunk.match is RecitationMatch.Matched)
        assertEquals(setOf("1:1:1", "1:1:4"), chunk.wordFeedbackById().keys)
        assertEquals(RecitationCursor(1, 1, 3), chunk.cursor)
    }

    @Test
    fun `level events track the gate opening and settling on a waqf`() = runBlocking {
        service = FakeAiService().start()

        val events = runSession(FakeCapture(frameCount = 4)) {
            awaitUntil("audio streamed") { service.binaryFrameCount.get() == 4 }
        }

        val levels = events.filterIsInstance<LiveRecitationEvent.Level>()
        assertTrue("no level events while audio flowed", levels.any { it.isSpeaking && it.amplitude > 0f })
        assertTrue("the meter never settled on the waqf", levels.any { !it.isSpeaking })
    }

    @Test
    fun `forwarded silence reaches the server without reporting as speech`() = runBlocking {
        service = FakeAiService().start()

        val capture = FakeCapture(frameCount = 3, isSpeech = false)
        val events = runSession(capture) {
            awaitUntil("audio streamed") { service.binaryFrameCount.get() == 3 }
        }

        assertEquals(3, service.binaryFrameCount.get())

        val levels = events.filterIsInstance<LiveRecitationEvent.Level>()
        assertTrue("no level events were emitted at all", levels.isNotEmpty())
        assertTrue(
            "forwarded silence was reported as speech",
            levels.none { it.isSpeaking },
        )
    }

    @Test
    fun `a seek is forwarded to the running session`() = runBlocking {
        service = FakeAiService().start()

        runSession(FakeCapture(frameCount = 2)) { controls ->
            controls.emit(RecitationControl.Seek(RecitationCursor(sura = 2, aya = 255)))
        }

        val seek = service.textMessages.single { it.contains("\"seek\"") }
        assertTrue(seek.contains("\"sura\":2"))
        assertTrue(seek.contains("\"aya\":255"))
    }

    @Test
    fun `an engine substitution surfaces on the started event`() = runBlocking {
        service = FakeAiService(ackEngine = "real").start()

        val events = runSession(
            capture = FakeCapture(frameCount = 1),
            config = LiveRecitationConfig(start = RecitationCursor(1, 1), engine = "zipformer"),
        )

        val started = events.filterIsInstance<LiveRecitationEvent.Started>().single()
        assertEquals("real", started.engine)
        assertTrue("a silent engine substitution went unreported", started.engineSubstituted)
    }

    @Test
    fun `local words from the recognizer arrive as LocalWord events`() = runBlocking {
        service = FakeAiService().start()
        val capture = FakeCapture(frameCount = 3)
        val recognizer = FakeLocalSpeechRecognizer(scriptedWords = listOf("الله", "الرحمن"))
        val controls = MutableSharedFlow<RecitationControl>()
        val events = mutableListOf<LiveRecitationEvent>()
        withTimeout(TIMEOUT_MS) {
            coroutineScope {
                val session = launch {
                    repositoryFor(capture, recognizer).session(
                        LiveRecitationConfig(start = RecitationCursor(1, 1)),
                        controls,
                    ).map { it.getOrNull()!! }.toList(events)
                }
                awaitUntil("session started") { events.any { it is LiveRecitationEvent.Started } }
                awaitUntil("local words arrived") {
                    events.count { it is LiveRecitationEvent.LocalWord } >= 2
                }
                controls.emit(RecitationControl.Finish)
                session.join()
            }
        }

        assertEquals(
            listOf("الله", "الرحمن"),
            events.filterIsInstance<LiveRecitationEvent.LocalWord>().map { it.word },
        )
    }

    @Test
    fun `a failing local recognizer does not fail the session`() = runBlocking {
        service = FakeAiService().start()
        val capture = FakeCapture(frameCount = 3)
        val recognizer = FakeLocalSpeechRecognizer(failure = IllegalStateException("boom"))

        val events = runSession(capture, localSpeechRecognizer = recognizer) {
            awaitUntil("audio streamed") { service.binaryFrameCount.get() == 3 }
        }

        assertTrue(
            "a broken recognizer must never produce a LocalWord event",
            events.none { it is LiveRecitationEvent.LocalWord },
        )
        assertTrue("the session must survive a local recognizer failure", events.last() is LiveRecitationEvent.Finished)
    }

    @Test
    fun `stopping before the handshake lands ends cleanly without opening the microphone`() = runBlocking {
        service = FakeAiService().start()
        val capture = FakeCapture(frameCount = 3)
        val controls = MutableSharedFlow<RecitationControl>(replay = 1)
        controls.emit(RecitationControl.Finish)

        val events = withTimeout(TIMEOUT_MS) {
            repositoryFor(capture).session(
                config = LiveRecitationConfig(start = RecitationCursor(1, 1)),
                controls = controls,
            ).map { it.getOrNull()!! }.toList()
        }

        assertTrue("session did not end cleanly", events.last() is LiveRecitationEvent.Finished)
        assertEquals("audio was streamed after stopping", 0, service.binaryFrameCount.get())
        assertEquals("the microphone was opened for an abandoned session", 0L, capture.releasedAt)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val AWAIT_MS = 5_000L
    }
}
