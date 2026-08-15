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


class LiveRecitationSocket(
    private val client: HttpClient,
    private val config: AiServiceConfig,
    private val json: Json = ProtocolJson,
) {

    fun open(
        start: StartSessionDto,
        commands: Flow<LiveSessionCommand>,
    ): Flow<LiveSessionEvent> = channelFlow {
        val latency = LiveLatencyProbe()

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


            val pump = launch { pumpCommands(commands, latency) }
            val sawDone = try {
                readEvents(latency) { event -> send(event) }
            } finally {
                pump.cancel()
            }

            if (!sawDone) {
                
                
                
                val reason = closeReason.await()
                Log.w(TAG, "Session ended without 'done' (close=${reason?.code} ${reason?.message})")
                throw LiveSessionException(
                    message = describeClose(reason?.code, reason?.message),
                    closeCode = reason?.code,
                )
            }
        }
        
        close()
        awaitClose()
    }

    





 
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

    private suspend fun io.ktor.websocket.WebSocketSession.pumpCommands(
        commands: Flow<LiveSessionCommand>,
        latency: LiveLatencyProbe,
    ) {
        commands.collect { command ->
            when (command) {
                is LiveSessionCommand.Audio -> {


                    outgoing.send(Frame.Binary(true, PcmCodec.toLittleEndianBytes(command.frame.samples)))
                    // After the send, not before: the mark must record when the audio actually
                    // left, otherwise a backed-up outgoing queue would be invisible here and
                    // charged to the server instead.
                    latency.onAudioSent(command.frame)
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

    



 
    private suspend fun io.ktor.websocket.WebSocketSession.readEvents(
        latency: LiveLatencyProbe,
        emit: suspend (LiveSessionEvent) -> Unit,
    ): Boolean {
        for (frame in incoming) {
            if (frame !is Frame.Text) continue
            val text = frame.readText()
            when (val type = json.decodeFromString(MessageEnvelopeDto.serializer(), text).type) {
                TYPE_FEEDBACK -> {
                    val envelope = json.decodeFromString(FeedbackEnvelopeDto.serializer(), text)
                    latency.onFeedback(
                        chunkSeq = envelope.chunkSeq,
                        spanEndSec = envelope.audioSpanSec.lastOrNull(),
                        forcedCut = envelope.forcedCut,
                    )
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







 
internal val ProtocolJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
}
