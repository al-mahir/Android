package com.example.mushaf.data.recite.remote

import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.recite.audio.PcmCodec
import com.example.mushaf.data.recite.remote.dto.EndSessionDto
import com.example.mushaf.data.recite.remote.dto.FeedbackEnvelopeDto
import com.example.mushaf.data.recite.remote.dto.MessageEnvelopeDto
import com.example.mushaf.data.recite.remote.dto.SeekDto
import com.example.mushaf.data.recite.remote.dto.SessionAckDto
import com.example.mushaf.data.recite.remote.dto.StartSessionDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * One live recitation session over `WS /ws/session`.
 *
 * The exchange, in the order the server requires:
 *
 * ```
 * connect  ->  send {"type":"start", …}  ->  recv {"type":"session", …}
 *          ->  binary PCM frames …       <-  {"type":"feedback"} per waqf
 *          ->  {"type":"end"}            <-  {"type":"done"}  ->  close
 * ```
 *
 * **Ordering is enforced, not assumed.** The start message must be the first frame and must be
 * text JSON. Worse, two known server bugs (MOBILE_INTEGRATION.md §7) turn a violation into an
 * unhandled exception and a **1006** close rather than the documented clean 1002 — so a client
 * that starts streaming too early sees a generic network error and debugs the wrong layer. This
 * implementation therefore waits for the ack before sending a single byte of audio. It costs one
 * round trip at session start and removes the entire class of bug.
 */
class LiveRecitationSocket(
    private val client: HttpClient,
    private val config: AiServiceConfig,
    private val json: Json = ProtocolJson,
) {

    /**
     * Runs a session and streams its events.
     *
     * Cold: nothing connects until collection starts, and the socket closes when collection
     * stops, however it stops. [commands] is consumed only after the handshake succeeds.
     *
     * The caller ends a session by emitting [LiveSessionCommand.End] and then completing
     * [commands] — the flow stays open afterwards to receive the flush feedback and `done`.
     * Simply cancelling instead loses the final chunk of the recitation.
     */
    fun open(
        start: StartSessionDto,
        commands: Flow<LiveSessionCommand>,
    ): Flow<LiveSessionEvent> = channelFlow {
        client.webSocket(urlString = config.sessionUrl) {
            outgoing.send(Frame.Text(json.encodeToString(StartSessionDto.serializer(), start)))
            Log.d(TAG, "Session start sent to ${config.sessionUrl} (engine=${start.engine ?: "server default"})")

            val ack = awaitAck()
            send(
                LiveSessionEvent.Started(
                    sessionId = ack.sessionId,
                    engine = ack.engine,
                    sampleRate = ack.sampleRate,
                    requestedEngine = start.engine,
                ),
            )

            // Only now is it safe to put binary on the wire.
            val pump = launch { pumpCommands(commands) }
            val sawDone = try {
                readEvents { event -> send(event) }
            } finally {
                pump.cancel()
            }

            if (!sawDone) {
                // The socket ended without `done`. The close code is the only diagnostic the
                // service gives, and the three cases need different responses, so it is
                // reported rather than swallowed as a flow that merely completed.
                val reason = closeReason.await()
                Log.w(TAG, "Session ended without 'done' (close=${reason?.code} ${reason?.message})")
                throw LiveSessionException(
                    message = describeClose(reason?.code, reason?.message),
                    closeCode = reason?.code,
                )
            }
        }
        // webSocket() returns once the server closed; nothing further can arrive.
        close()
        awaitClose()
    }

    /**
     * Reads until the session ack arrives.
     *
     * Anything before it is a protocol violation on the server's side rather than something to
     * paper over, so it fails loudly instead of streaming audio into a session that may not
     * exist.
     */
    private suspend fun io.ktor.websocket.WebSocketSession.awaitAck(): SessionAckDto {
        for (frame in incoming) {
            if (frame !is Frame.Text) continue
            val text = frame.readText()
            val type = json.decodeFromString(MessageEnvelopeDto.serializer(), text).type
            if (type == TYPE_SESSION) {
                return json.decodeFromString(SessionAckDto.serializer(), text).also {
                    Log.i(TAG, "Session ${it.sessionId} ack: engine=${it.engine}, rate=${it.sampleRate}")
                }
            }
            Log.w(TAG, "Ignoring '$type' received before the session ack")
        }
        throw LiveSessionException("Socket closed before the session ack arrived")
    }

    private suspend fun io.ktor.websocket.WebSocketSession.pumpCommands(commands: Flow<LiveSessionCommand>) {
        commands.collect { command ->
            when (command) {
                is LiveSessionCommand.Audio -> {
                    // Binary, never text: the string overload produces a text frame, which the
                    // server treats as a control message and silently ignores.
                    outgoing.send(Frame.Binary(true, PcmCodec.toLittleEndianBytes(command.frame.samples)))
                }

                is LiveSessionCommand.Seek -> {
                    val seek = SeekDto(sura = command.sura, aya = command.aya, wordIdx = command.wordIdx)
                    outgoing.send(Frame.Text(json.encodeToString(SeekDto.serializer(), seek)))
                    Log.d(TAG, "Seek → ${command.sura}:${command.aya}:${command.wordIdx}")
                }

                LiveSessionCommand.End -> {
                    outgoing.send(Frame.Text(json.encodeToString(EndSessionDto.serializer(), EndSessionDto())))
                    Log.d(TAG, "End sent; awaiting flush and done")
                }
            }
        }
    }

    /**
     * [emit] hands events to the surrounding flow; the read loop itself owns no channel.
     *
     * Returns true if the server said `done`, false if the socket ended first.
     */
    private suspend fun io.ktor.websocket.WebSocketSession.readEvents(
        emit: suspend (LiveSessionEvent) -> Unit,
    ): Boolean {
        for (frame in incoming) {
            if (frame !is Frame.Text) continue
            val text = frame.readText()
            when (val type = json.decodeFromString(MessageEnvelopeDto.serializer(), text).type) {
                TYPE_FEEDBACK -> {
                    val envelope = json.decodeFromString(FeedbackEnvelopeDto.serializer(), text)
                    Log.d(
                        TAG,
                        "Feedback #${envelope.chunkSeq}: status=${envelope.feedback?.status} " +
                            "words=${envelope.feedback?.words?.size ?: 0} " +
                            "cursor=${envelope.cursor?.sura}:${envelope.cursor?.aya}:${envelope.cursor?.wordIdx}" +
                            if (envelope.forcedCut) " FORCED_CUT" else "",
                    )
                    emit(LiveSessionEvent.Feedback(envelope))
                }

                TYPE_DONE -> {
                    emit(LiveSessionEvent.Done)
                    return true
                }

                else -> Log.w(TAG, "Ignoring unknown message type '$type'")
            }
        }
        return false
    }

    /** Turns a close code into the action the caller should take (API.md §5.9). */
    private fun describeClose(code: Short?, message: String?): String = when (code?.toInt()) {
        CLOSE_PROTOCOL_ERROR ->
            "The server rejected the first frame as invalid JSON (1002). This is a client " +
                "protocol bug, not a transient one."
        CLOSE_ABNORMAL, null ->
            "The connection dropped before the session finished (1006 or no close frame). " +
                "Reconnect and resume from the last cursor."
        else -> "Session closed with $code${message?.let { ": $it" }.orEmpty()} before 'done'."
    }

    private companion object {
        const val TAG = MushafLog.TAG
        const val TYPE_SESSION = "session"
        const val TYPE_FEEDBACK = "feedback"
        const val TYPE_DONE = "done"

        const val CLOSE_PROTOCOL_ERROR = 1002
        const val CLOSE_ABNORMAL = 1006
    }
}

/**
 * Lenient on the way in, sparse on the way out.
 *
 * `ignoreUnknownKeys` so a server that grows a field does not drop a live session, and
 * `explicitNulls = false` so an unset field in the start message is *absent* rather than an
 * explicit `null` — the contract reads absence as "use the server default".
 */
internal val ProtocolJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
}
