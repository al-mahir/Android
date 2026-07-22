package com.example.mushaf.data.recite

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger










 
class FakeAiService(
     
    private val feedbackPayloads: List<String> = emptyList(),
     
    private val flushOnEnd: String? = null,
    





 
    private val ackEngine: String = "real",
) {
    private var server: EmbeddedServer<*, *>? = null

     
    var port: Int = 0
        private set

    

    @Volatile
    var firstFrameWasText: Boolean? = null
        private set

    @Volatile
    var startMessage: String? = null
        private set

    val binaryFrameCount = AtomicInteger()
    val binaryByteCount = AtomicInteger()
    val textMessages = mutableListOf<String>()

    @Volatile
    var receivedEnd = false
        private set

    fun start(): FakeAiService {
        port = freePort()
        
        val engineName = ackEngine
        server = embeddedServer(CIO, port = port) {
            install(WebSockets)
            routing {
                webSocket("/ws/session") {
                    var sentFeedback = false

                    for (frame in incoming) {
                        if (firstFrameWasText == null) {
                            firstFrameWasText = frame is Frame.Text
                        }

                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                synchronized(textMessages) { textMessages += text }

                                when {
                                    text.contains("\"start\"") -> {
                                        startMessage = text
                                        outgoing.send(
                                            Frame.Text(
                                                """{"type":"session","session_id":"stub-1",""" +
                                                    """"engine":"$engineName","sample_rate":16000}""",
                                            ),
                                        )
                                    }

                                    text.contains("\"end\"") -> {
                                        receivedEnd = true
                                        flushOnEnd?.let { outgoing.send(Frame.Text(it)) }
                                        outgoing.send(Frame.Text("""{"type":"done"}"""))
                                        return@webSocket
                                    }
                                }
                            }

                            is Frame.Binary -> {
                                binaryFrameCount.incrementAndGet()
                                binaryByteCount.addAndGet(frame.data.size)
                                
                                
                                if (!sentFeedback && feedbackPayloads.isNotEmpty()) {
                                    sentFeedback = true
                                    feedbackPayloads.forEach { outgoing.send(Frame.Text(it)) }
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }.also { it.start(wait = false) }
        return this
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 500)
        server = null
    }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }
}

 
const val FATIHA_FEEDBACK_JSON: String = """
{
  "type": "feedback",
  "chunk_seq": 0,
  "audio_span_sec": [0.168, 19.296],
  "forced_cut": true,
  "phonemes": "بِسمِللَااهِررَحمَاانِررَحِۦۦم",
  "feedback": {
    "status": "ok",
    "span": {"sura": 1, "aya": 1, "word_idx": 0},
    "end": {"sura": 1, "aya": 1, "word_idx": 3},
    "uthmani_text": "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
    "predicted_phonemes": "...",
    "reference_phonemes": "...",
    "words": [
      {"sura": 1, "aya": 1, "word_idx": 0, "uthmani": "بِسْمِ",
       "status": "correct", "errors": [], "trimmed": false},
      {"sura": 1, "aya": 1, "word_idx": 3, "uthmani": "ٱلرَّحِيمِ",
       "status": "correct", "errors": [], "trimmed": true}
    ],
    "candidates": [],
    "non_verse": []
  },
  "cursor": {"sura": 1, "aya": 1, "word_idx": 3}
}
"""

 
const val MADD_ERROR_FEEDBACK_JSON: String = """
{
  "type": "feedback",
  "chunk_seq": 1,
  "audio_span_sec": [19.3, 24.0],
  "forced_cut": false,
  "feedback": {
    "status": "ok",
    "words": [
      {"sura": 1, "aya": 1, "word_idx": 2, "uthmani": "ٱلرَّحْمَـٰنِ",
       "status": "error", "trimmed": false,
       "errors": [
         {"error_type": "tajweed", "speech_error_type": "replace",
          "uthmani_pos": [25, 26], "ph_pos": [18, 20], "pred_ph_pos": [18, 21],
          "expected_ph": "اا", "predicted_ph": "ااا",
          "expected_len": 2, "predicted_len": 3,
          "tajweed_rules": [
            {"name_ar": "المد الطبيعي", "name_en": "Normal Madd",
             "golden_len": 2, "correctness_type": "count", "tag": "alif"}
          ],
          "confidence": 0.97}
       ]}
    ]
  },
  "cursor": {"sura": 1, "aya": 1, "word_idx": 2}
}
"""

 
const val AMBIGUOUS_FEEDBACK_JSON: String = """
{
  "type": "feedback",
  "chunk_seq": 0,
  "audio_span_sec": [0.0, 3.0],
  "feedback": {
    "status": "ambiguous",
    "words": [],
    "candidates": [
      {"sura": 1, "aya": 1, "word_idx": 0,
       "uthmani_text": "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
       "end": {"sura": 1, "aya": 1, "word_idx": 3}},
      {"sura": 27, "aya": 30, "word_idx": 3, "uthmani_text": "..."}
    ]
  },
  "cursor": null
}
"""
