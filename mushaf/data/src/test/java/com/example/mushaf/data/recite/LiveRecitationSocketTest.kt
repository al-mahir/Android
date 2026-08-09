package com.example.mushaf.data.recite

import com.example.mushaf.data.recite.remote.AiServiceConfig
import com.example.mushaf.data.recite.remote.LiveRecitationSocket
import com.example.mushaf.data.recite.remote.LiveSessionCommand
import com.example.mushaf.data.recite.remote.LiveSessionEvent
import com.example.mushaf.data.recite.remote.dto.StartSessionDto
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test






 
class LiveRecitationSocketTest {

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

    private fun socketFor(service: FakeAiService) =
        LiveRecitationSocket(client, AiServiceConfig(authority = "localhost:${service.port}", secure = false))

    private fun audioFrames(count: Int): List<LiveSessionCommand.Audio> = List(count) { index ->
        LiveSessionCommand.Audio(
            AudioFrame(
                samples = ShortArray(RecitationAudioFormat.FRAME_SAMPLES) { 1_000 },
                startSample = index.toLong() * RecitationAudioFormat.FRAME_SAMPLES,
            ),
        )
    }

    private fun commandsOf(vararg commands: LiveSessionCommand): Flow<LiveSessionCommand> =
        flow { commands.forEach { emit(it) } }

    @Test
    fun `a session handshakes, streams audio, and closes on done`() = runBlocking {
        service = FakeAiService(feedbackPayloads = listOf(FATIHA_FEEDBACK_JSON)).start()

        val events = withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(sura = 1, aya = 1, wordIdx = 0),
                commands = commandsOf(*audioFrames(3).toTypedArray(), LiveSessionCommand.End),
            ).toList()
        }

        assertTrue(events.first() is LiveSessionEvent.Started)
        assertTrue("session never reported done", events.last() is LiveSessionEvent.Done)
        assertTrue(service.receivedEnd)
    }

    @Test
    fun `the first frame is text JSON, never audio`() = runBlocking {
        
        
        service = FakeAiService().start()

        withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(sura = 2, aya = 255),
                commands = commandsOf(*audioFrames(5).toTypedArray(), LiveSessionCommand.End),
            ).toList()
        }

        assertEquals("first frame was binary", true, service.firstFrameWasText)
        val start = service.startMessage
        assertNotNull("no start message was ever received", start)
        assertTrue("start message was not first: $start", start!!.contains("\"type\":\"start\""))
        assertTrue(start.contains("\"sura\":2"))
        assertTrue(start.contains("\"aya\":255"))
    }

    @Test
    fun `unset start fields are omitted rather than sent as null`() = runBlocking {
        
        
        service = FakeAiService().start()

        withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(sura = 1, aya = 1, wordIdx = 0),
                commands = commandsOf(LiveSessionCommand.End),
            ).toList()
        }

        val start = service.startMessage.orEmpty()
        assertFalse("nulls were serialised: $start", start.contains("null"))
        assertFalse("unset engine was sent: $start", start.contains("\"engine\""))
        assertFalse("unset rules were sent: $start", start.contains("\"rules\""))
    }

    @Test
    fun `audio goes out as binary frames of the expected size`() = runBlocking {
        service = FakeAiService().start()

        withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(sura = 1, aya = 1),
                commands = commandsOf(*audioFrames(4).toTypedArray(), LiveSessionCommand.End),
            ).toList()
        }

        assertEquals(4, service.binaryFrameCount.get())
        
        
        assertEquals(4 * RecitationAudioFormat.FRAME_BYTES, service.binaryByteCount.get())
    }

    @Test
    fun `feedback is parsed with its cursor and trimmed flags intact`() = runBlocking {
        service = FakeAiService(feedbackPayloads = listOf(FATIHA_FEEDBACK_JSON)).start()

        val events = withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(sura = 1, aya = 1),
                commands = commandsOf(*audioFrames(2).toTypedArray(), LiveSessionCommand.End),
            ).toList()
        }

        val feedback = events.filterIsInstance<LiveSessionEvent.Feedback>().single().envelope
        assertEquals(0, feedback.chunkSeq)
        assertTrue(feedback.forcedCut)
        assertEquals("ok", feedback.feedback?.status)
        assertEquals(3, feedback.cursor?.wordIdx)

        val words = feedback.feedback?.words.orEmpty()
        assertEquals(2, words.size)
        
        val trimmed = words.single { it.trimmed }
        assertEquals("correct", trimmed.status)
        assertEquals(3, trimmed.wordIdx)
    }

    @Test
    fun `a tajweed finding survives the round trip with its rule and lengths`() = runBlocking {
        service = FakeAiService(feedbackPayloads = listOf(MADD_ERROR_FEEDBACK_JSON)).start()

        val events = withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(sura = 1, aya = 1),
                commands = commandsOf(*audioFrames(2).toTypedArray(), LiveSessionCommand.End),
            ).toList()
        }

        val word = events.filterIsInstance<LiveSessionEvent.Feedback>()
            .single().envelope.feedback!!.words.single()
        assertEquals("error", word.status)

        val error = word.errors.single()
        assertEquals("tajweed", error.errorType)
        assertEquals(2, error.expectedLen)
        assertEquals(3, error.predictedLen)
        assertEquals(0.97f, error.confidence!!, 0.0001f)
        
        assertEquals(listOf(25, 26), error.uthmaniPos)
        assertEquals("المد الطبيعي", error.tajweedRules.single().nameAr)
    }

    @Test
    fun `an ambiguous chunk carries candidates and asserts no words`() = runBlocking {
        service = FakeAiService(feedbackPayloads = listOf(AMBIGUOUS_FEEDBACK_JSON)).start()

        val events = withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(),
                commands = commandsOf(*audioFrames(2).toTypedArray(), LiveSessionCommand.End),
            ).toList()
        }

        val feedback = events.filterIsInstance<LiveSessionEvent.Feedback>().single().envelope.feedback!!
        assertEquals("ambiguous", feedback.status)
        assertTrue("ambiguous chunk asserted words", feedback.words.isEmpty())
        assertEquals(2, feedback.candidates.size)
        
        assertNotNull(feedback.candidates.first().uthmaniText)
    }

    @Test
    fun `an engine substitution is visible in the ack`() = runBlocking {
        
        service = FakeAiService(ackEngine = "real").start()

        val events = withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(sura = 1, aya = 1, engine = "zipformer"),
                commands = commandsOf(LiveSessionCommand.End),
            ).toList()
        }

        val started = events.filterIsInstance<LiveSessionEvent.Started>().single()
        assertEquals("real", started.engine)
        assertTrue("substitution went unnoticed", started.engineSubstituted)
    }

    @Test
    fun `a matching engine is not reported as substituted`() = runBlocking {
        service = FakeAiService(ackEngine = "real").start()

        val events = withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(sura = 1, aya = 1, engine = "real"),
                commands = commandsOf(LiveSessionCommand.End),
            ).toList()
        }

        assertFalse(events.filterIsInstance<LiveSessionEvent.Started>().single().engineSubstituted)
    }

    @Test
    fun `a seek is sent as text mid-session`() = runBlocking {
        service = FakeAiService().start()

        withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(sura = 1, aya = 1),
                commands = commandsOf(
                    audioFrames(1).first(),
                    LiveSessionCommand.Seek(sura = 2, aya = 255, wordIdx = 0),
                    LiveSessionCommand.End,
                ),
            ).toList()
        }

        val seek = service.textMessages.single { it.contains("\"seek\"") }
        assertTrue(seek.contains("\"sura\":2"))
        assertTrue(seek.contains("\"aya\":255"))
    }

    @Test
    fun `the flush feedback after end is not lost`() = runBlocking {
        
        
        service = FakeAiService(flushOnEnd = MADD_ERROR_FEEDBACK_JSON).start()

        val events = withTimeout(TIMEOUT_MS) {
            socketFor(service).open(
                start = StartSessionDto(sura = 1, aya = 1),
                commands = commandsOf(*audioFrames(2).toTypedArray(), LiveSessionCommand.End),
            ).toList()
        }

        assertEquals(1, events.filterIsInstance<LiveSessionEvent.Feedback>().size)
        assertTrue(events.last() is LiveSessionEvent.Done)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
