package com.example.mushaf.data.recite

import com.example.mushaf.data.recite.remote.AiServiceApi
import com.example.mushaf.data.recite.remote.AiServiceConfig
import com.example.mushaf.data.recite.remote.LiveRecitationSocket
import com.example.mushaf.data.recite.remote.LiveSessionCommand
import com.example.mushaf.data.recite.remote.LiveSessionEvent
import com.example.mushaf.data.recite.remote.dto.StartSessionDto
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.Socket
















 
class LiveServerSmokeTest {

    private lateinit var client: HttpClient
    private val config = AiServiceConfig(authority = AUTHORITY, secure = false)

    @Before
    fun setUp() {
        assumeTrue(
            "No Al-Mahir server on $AUTHORITY — skipping. Start it, or pass -DalmahirServer=host:port.",
            isReachable(AUTHORITY),
        )
        client = HttpClient(OkHttp) {
            install(WebSockets)
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @After
    fun tearDown() {
        if (::client.isInitialized) client.close()
    }

     
    @Test
    fun `health reports a healthy service and its engines`() = runBlocking {
        val health = withTimeout(TIMEOUT_MS) { AiServiceApi(client, config).health() }

        println("[live] health: $health")
        assertEquals("healthy", health.status)
        assertTrue("no engines reported", health.availableEngines.isNotEmpty())
    }

    @Test
    fun `discovery endpoints parse into the DTOs the app builds its settings from`() = runBlocking {
        val api = AiServiceApi(client, config)

        val rules = withTimeout(TIMEOUT_MS) { api.tajweedRules() }
        val schema = withTimeout(TIMEOUT_MS) { api.moshafSchema() }

        println("[live] ${rules.rules.size} tajweed rules, ${schema.fields.size} moshaf fields")
        assertTrue("no tajweed rules returned", rules.rules.isNotEmpty())
        assertTrue("no moshaf fields returned", schema.fields.isNotEmpty())
        
        assertTrue(schema.fields.all { it.key.isNotBlank() })
    }

    


 
    @Test
    fun `a live session handshakes and reaches done`() = runBlocking {
        val frames = recitationFrames()
        println("[live] streaming ${frames.size} frames (${frames.size * RecitationAudioFormat.FRAME_DURATION_MS}ms)")

        val events = withTimeout(SESSION_TIMEOUT_MS) {
            LiveRecitationSocket(client, config).open(
                start = StartSessionDto(sura = START_SURA, aya = START_AYA, wordIdx = 0),
                commands = commandsOf(frames),
            ).toList()
        }

        val started = events.filterIsInstance<LiveSessionEvent.Started>().single()
        println("[live] session ${started.sessionId} on engine '${started.engine}'")

        events.filterIsInstance<LiveSessionEvent.Feedback>().forEach { (envelope) ->
            println(
                "[live] chunk ${envelope.chunkSeq}: status=${envelope.feedback?.status} " +
                    "text=${envelope.feedback?.uthmaniText} " +
                    "cursor=${envelope.cursor?.sura}:${envelope.cursor?.aya}:${envelope.cursor?.wordIdx}",
            )
            envelope.feedback?.words?.forEach { word ->
                println("[live]    ${word.uthmani} → ${word.status}${if (word.trimmed) " (trimmed)" else ""}")
            }
        }

        assertTrue("session never reached done", events.last() is LiveSessionEvent.Done)
    }

    private fun commandsOf(frames: List<AudioFrame>): Flow<LiveSessionCommand> = flow {
        frames.forEach { emit(LiveSessionCommand.Audio(it)) }
        emit(LiveSessionCommand.End)
    }

    



 
    private fun recitationFrames(): List<AudioFrame> {
        val wavPath = System.getProperty(WAV_PROPERTY)
        if (wavPath != null) {
            val file = File(wavPath)
            require(file.isFile) { "-D$WAV_PROPERTY=$wavPath is not a file" }
            return runBlocking {
                com.example.mushaf.data.recite.audio.WavPcmRecorder(open = { file.inputStream() })
                    .record().toList()
            }
        }

        var sample = 0L
        fun frame(amplitude: Int) = AudioFrame(
            samples = ShortArray(RecitationAudioFormat.FRAME_SAMPLES) { index ->
                if (index % 2 == 0) amplitude.toShort() else (-amplitude).toShort()
            },
            startSample = sample,
        ).also { sample += RecitationAudioFormat.FRAME_SAMPLES }

        return List(20) { frame(6_000) } + List(10) { frame(0) }
    }

    private fun isReachable(authority: String): Boolean {
        val host = authority.substringBefore(':')
        val port = authority.substringAfter(':', "8100").toIntOrNull() ?: return false
        return runCatching {
            Socket().use { it.connect(java.net.InetSocketAddress(host, port), PROBE_TIMEOUT_MS); true }
        }.getOrDefault(false)
    }

    private companion object {
        const val SERVER_PROPERTY = "almahirServer"
        const val WAV_PROPERTY = "almahirWav"

         
        val AUTHORITY: String = System.getProperty(SERVER_PROPERTY) ?: "localhost:8100"

        const val START_SURA = 1
        const val START_AYA = 1
        const val PROBE_TIMEOUT_MS = 300
        const val TIMEOUT_MS = 30_000L

         
        const val SESSION_TIMEOUT_MS = 120_000L
    }
}
