package com.iti.data.exam

import android.util.Log
import com.iti.data.BuildConfig
import com.iti.data.exam.dto.CursorDto
import com.iti.data.exam.dto.DoneDto
import com.iti.data.exam.dto.EndSessionDto
import com.iti.data.exam.dto.FeedbackDto
import com.iti.data.exam.dto.IncomingMessageDto
import com.iti.data.exam.dto.SeekDto
import com.iti.data.exam.dto.StartSessionDto
import com.iti.data.exam.dto.WordDto
import com.iti.domain.model.exam.WordFeedback
import com.iti.domain.model.exam.WordStatus
import com.iti.domain.repository.exam.ExamDataSource
import com.iti.domain.repository.exam.ExamSessionHandle
import com.iti.domain.repository.exam.FeedbackEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.wss
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.iti.data.core.token.TokenStore
import com.iti.data.core.token.getAccessToken
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders.Authorization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "KtorExamDataSource"

@Serializable
private data class ProgressDto(
    @SerialName("type") val type: String,
    @SerialName("confirmed") val confirmed: List<CursorDto> = emptyList(),
    @SerialName("cursor") val cursor: CursorDto? = null
)

class KtorExamDataSource(
    private val tokenStore: TokenStore,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) : ExamDataSource {

    override suspend fun openSession(
        surah: Int,
        ayah: Int,
        strictness: String,
        onFeedback: suspend (FeedbackEvent) -> Unit,
        onDone: suspend (FeedbackEvent) -> Unit,
        onError: suspend (Throwable) -> Unit,
    ): ExamSessionHandle {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val client = HttpClient(OkHttp) { install(WebSockets) }
        var wsSession: DefaultClientWebSocketSession? = null

        scope.launch {
            try {
                val token = tokenStore.getAccessToken()
                Log.d(TAG, "Opening WS session → wss://${BuildConfig.AI_SERVICE_HOST}/ws/session  sura=$surah aya=$ayah")

                client.wss(
                    host = BuildConfig.AI_SERVICE_HOST,
                    path = "/ws/session",
                    request = {
                        if (token != null) header(Authorization, "Bearer _pmUwNKpcrUxY1UiYMmcGyP4HV3tSWQxP6JO1pbO8gw")
                        header("ngrok-skip-browser-warning", "1")
                    }
                ) {
                    wsSession = this
                    val startMsg = json.encodeToString(StartSessionDto(sura = surah, aya = ayah, strictness = strictness))
                    send(Frame.Text(startMsg))

                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val text = frame.readText()

                        try {
                            val base = json.decodeFromString<IncomingMessageDto>(text)
                            when (base.type) {
                                "session" -> Log.d(TAG, "Session ACK received")

                                "progress" -> {
                                    val dto = json.decodeFromString<ProgressDto>(text)
                                    val event = FeedbackEvent(
                                        words = emptyList(),
                                        feedbackStatus = "progress",
                                        cursorSurah = dto.cursor?.sura ?: surah,
                                        cursorAyah = dto.cursor?.aya ?: ayah,
                                        cursorWord = dto.cursor?.word ?: 0,
                                        confirmedProgressWords = dto.confirmed.size
                                    )
                                    onFeedback(event)
                                }

                                "feedback" -> {
                                    val dto = json.decodeFromString<FeedbackDto>(text)
                                    val event = dto.toFeedbackEvent(surah, ayah)
                                    onFeedback(event)
                                }

                                "done" -> {
                                    val dto = json.decodeFromString<DoneDto>(text)
                                    val event = FeedbackEvent(
                                        words = emptyList(),
                                        feedbackStatus = "done",
                                        cursorSurah = dto.cursor?.sura ?: surah,
                                        cursorAyah = dto.cursor?.aya ?: ayah,
                                        cursorWord = dto.cursor?.word ?: 0,
                                        isDone = true,
                                    )
                                    onDone(event)
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse frame: $text", e)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    onError(e)
                }
            } finally {
                client.close()
            }
        }

        return object : ExamSessionHandle {
            override suspend fun sendAudio(pcm16Bytes: ByteArray) { wsSession?.send(Frame.Binary(fin = true, data = pcm16Bytes)) }
            override suspend fun sendEnd() { wsSession?.send(Frame.Text(json.encodeToString(EndSessionDto()))) }
            override suspend fun seek(surah: Int, ayah: Int, wordIdx: Int) { wsSession?.send(Frame.Text(json.encodeToString(SeekDto(sura = surah, aya = ayah, wordIdx = wordIdx)))) }
            override fun close() {
                scope.launch { try { wsSession?.close(CloseReason(CloseReason.Codes.NORMAL, "question ended")) } catch (_: Exception) {} }
                scope.cancel()
                client.close()
            }
        }
    }

    private fun FeedbackDto.toFeedbackEvent(fallbackSurah: Int, fallbackAyah: Int): FeedbackEvent {
        val payload = feedback
        return FeedbackEvent(
            words = payload?.words?.map { it.toWordFeedback() } ?: emptyList(),
            uthmaniText = payload?.uthmaniText,
            feedbackStatus = payload?.status ?: "ok",
            cursorSurah = cursor?.sura ?: fallbackSurah,
            cursorAyah = cursor?.aya ?: fallbackAyah,
            cursorWord = cursor?.word ?: 0,
            isDone = false,
            confirmedProgressWords = 0,
            candidates = payload?.candidates?.map {
                com.iti.domain.repository.exam.FeedbackCandidate(surah = it.sura, ayah = it.aya, uthmaniText = it.uthmaniText.orEmpty())
            } ?: emptyList(),
            nonVerse = payload?.nonVerse ?: emptyList(),
        )
    }

    private fun WordDto.toWordFeedback(): WordFeedback {
        val resolvedStatus = if (trimmed) WordStatus.TRIMMED else {
            when (status) {
                "correct" -> WordStatus.CORRECT
                "almost"  -> WordStatus.ALMOST
                "error"   -> WordStatus.ERROR
                else      -> WordStatus.PENDING
            }
        }

        val labels = errors.mapNotNull { err ->
            when {
                err.tajweedRules.isNotEmpty() -> err.tajweedRules.firstOrNull()?.nameAr ?: err.tajweedRules.firstOrNull()?.nameEn
                err.errorType != null -> err.errorType
                err.rule?.nameAr != null -> err.rule.nameAr
                err.rule?.name != null -> err.rule.name
                err.type != null -> err.type
                else -> null
            }
        }

        return WordFeedback(
            uthmani = uthmani,
            status = resolvedStatus,
            errorLabels = labels,
            trimmed = trimmed,
            primaryErrorType = errors.firstOrNull()?.errorType ?: errors.firstOrNull()?.type,
            surah = sura ?: 1, // MAPS EXACT POSITION
            ayah = aya ?: 1,   // MAPS EXACT POSITION
            wordIdx = wordIdx  // MAPS EXACT POSITION
        )
    }
}