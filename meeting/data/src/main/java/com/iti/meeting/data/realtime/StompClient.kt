package com.iti.meeting.data.realtime

import com.iti.domain.auth.MeetingAuthTokenProvider
import com.iti.meeting.data.remote.MeetingWsDestinations
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger


class StompClient(
    private val httpClient: HttpClient,
    private val wsUrl: String,
    private val tokenProvider: MeetingAuthTokenProvider,
) {
    private val scope = CoroutineScope(SupervisorJob())
    private val connectMutex = Mutex()
    private var connectionJob: Job? = null

    @Volatile
    private var activeSession: WebSocketSession? = null

    private val subscriptionIdSeq = AtomicInteger(0)
    private val subscriptionIds = mutableMapOf<String, String>() // destination -> subscription id
    private val destinationFlows = mutableMapOf<String, MutableSharedFlow<StompFrame>>()

    private val _reconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)


    val reconnected: SharedFlow<Unit> = _reconnected.asSharedFlow()

    suspend fun connect() {
        connectMutex.withLock {
            if (connectionJob?.isActive == true) return
            connectionJob = scope.launch { connectionLoop() }
        }
    }

    fun subscribe(destination: String): Flow<StompFrame> {
        val flow = destinationFlows.getOrPut(destination) { MutableSharedFlow(extraBufferCapacity = 16) }
        subscriptionIds.getOrPut(destination) { "sub-${subscriptionIdSeq.incrementAndGet()}" }
        activeSession?.let { session -> scope.launch { sendSubscribe(session, destination) } }
        return flow.asSharedFlow()
    }

    suspend fun send(destination: String, body: String) {
        val session = activeSession ?: return
        val frame = StompFrame(
            command = StompFrame.COMMAND_SEND,
            headers = mapOf(
                StompFrame.HEADER_DESTINATION to destination,
                StompFrame.HEADER_CONTENT_LENGTH to body.toByteArray(Charsets.UTF_8).size.toString(),
            ),
            body = body,
        )
        session.send(Frame.Text(StompFrameParser.serialize(frame)))
    }

    suspend fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        activeSession?.let { session ->
            runCatching {
                session.send(Frame.Text(StompFrameParser.serialize(StompFrame(StompFrame.COMMAND_DISCONNECT))))
                session.close()
            }
        }
        activeSession = null
        subscriptionIds.clear()
    }

    private suspend fun connectionLoop() {
        var attempt = 0
        while (scope.isActive) {
            val isReconnect = attempt > 0
            val connectedDurationMs = runCatching { openSession(isReconnect) }
                .onFailure { android.util.Log.e("StompClient", "Connection failed to wsUrl: $wsUrl", it) }
                .getOrDefault(0L)
            attempt = if (connectedDurationMs >= STABLE_CONNECTION_MS) 0 else attempt + 1
            if (attempt > 0) delay(backoffFor(attempt - 1))
        }
    }

    private suspend fun openSession(isReconnect: Boolean): Long {
        var connectedAtMs = 0L
        httpClient.webSocket(urlString = "$wsUrl${MeetingWsDestinations.CONNECT_PATH}") {
            val session: WebSocketSession = this
            activeSession = session
            connectedAtMs = System.currentTimeMillis()
            sendConnectFrame(session)
            
            // Issue every registered subscription (both new and reconnecting)
            subscriptionIds.keys.toList().forEach { destination -> sendSubscribe(session, destination) }
            
            if (isReconnect) {
                _reconnected.emit(Unit)
            }
            val heartbeatJob = launch { sendHeartbeats(session) }
            try {
                readLoop(session)
            } finally {
                heartbeatJob.cancel()
            }
        }
        activeSession = null
        return System.currentTimeMillis() - connectedAtMs
    }

    private suspend fun sendConnectFrame(session: WebSocketSession) {
        val token = tokenProvider.currentToken()
        val headers = buildMap {
            put(StompFrame.HEADER_ACCEPT_VERSION, "1.1,1.2")
            put(StompFrame.HEADER_HEART_BEAT, "$HEARTBEAT_MS,$HEARTBEAT_MS")
            if (token != null) put(StompFrame.HEADER_AUTHORIZATION, "Bearer $token")
        }
        session.send(Frame.Text(StompFrameParser.serialize(StompFrame(StompFrame.COMMAND_CONNECT, headers))))
    }

    private suspend fun sendSubscribe(session: WebSocketSession, destination: String) {
        val subscriptionId = subscriptionIds.getOrPut(destination) {
            "sub-${subscriptionIdSeq.incrementAndGet()}"
        }
        val frame = StompFrame(
            command = StompFrame.COMMAND_SUBSCRIBE,
            headers = mapOf(StompFrame.HEADER_ID to subscriptionId, StompFrame.HEADER_DESTINATION to destination),
        )
        session.send(Frame.Text(StompFrameParser.serialize(frame)))
    }

    private suspend fun sendHeartbeats(session: WebSocketSession) {
        while (session.isActive) {
            delay(HEARTBEAT_MS)
            runCatching { session.send(Frame.Text(StompFrameParser.serializeHeartbeat())) }
        }
    }

    private suspend fun readLoop(session: WebSocketSession) {
        for (frame in session.incoming) {
            if (frame !is Frame.Text) continue
            when (val incoming = StompFrameParser.parse(frame.readText())) {
                StompIncoming.Heartbeat -> Unit
                is StompIncoming.Frame -> handleFrame(incoming.frame)
            }
        }
    }

    private suspend fun handleFrame(frame: StompFrame) {
        if (frame.command != StompFrame.COMMAND_MESSAGE) return
        val destination = frame.headers[StompFrame.HEADER_DESTINATION] ?: return
        destinationFlows[destination]?.emit(frame)
    }

    private fun backoffFor(attemptIndex: Int): Long =
        BACKOFF_STEPS_MS.getOrElse(attemptIndex) { BACKOFF_STEPS_MS.last() }

    private companion object {
        const val HEARTBEAT_MS = 10_000L
        const val STABLE_CONNECTION_MS = 5_000L
        val BACKOFF_STEPS_MS = listOf(1_000L, 2_000L, 4_000L, 8_000L, 30_000L)
    }
}







