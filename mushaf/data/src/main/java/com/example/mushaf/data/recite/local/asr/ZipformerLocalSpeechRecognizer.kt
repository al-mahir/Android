package com.example.mushaf.data.recite.local.asr

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import com.example.mushaf.domain.model.recite.local.AsrModelState
import com.example.mushaf.domain.repository.LocalSpeechRecognizer
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineZipformer2CtcModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * On-device streaming ASR via sherpa-onnx's `OnlineRecognizer` (`Muno459/zipformer_p-quran`,
 * causal Zipformer2-CTC - see [AsrModelRepositoryImpl]'s doc for where the model comes from).
 * Genuinely streaming: [accept] feeds one 100ms frame at a time and [words] emits each newly
 * settled word as soon as the decoder is confident about it, not once per whole utterance.
 *
 * Settlement heuristic: a streaming CTC decoder's partial transcript can still revise its *last*
 * word as more audio arrives, but essentially never revises an earlier one once a later word has
 * started appearing - so everything except the last word in each decode cycle's result is safe
 * to emit immediately. This is the per-word design the old debounce-based approach (see
 * docs/features/06-taahud-speechrecognizer-status.md §6.4) needed but couldn't get from
 * `android.speech.SpeechRecognizer`'s black-box partial-result callback; here the decode loop is
 * ours, so it's straightforward. The endpoint detector (silence) flushes the final word too and
 * resets for the next utterance, mirroring sherpa-onnx's own official streaming demo.
 */
class ZipformerLocalSpeechRecognizer(
    private val asrModelRepository: AsrModelRepositoryImpl,
) : LocalSpeechRecognizer {

    private val _words = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val words: SharedFlow<String> = _words.asSharedFlow()

    override val isAvailable: Boolean
        get() = asrModelRepository.state.value is AsrModelState.Ready

    private val engineMutex = Mutex()
    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private var settledWordCount = 0

    override suspend fun accept(frame: AudioFrame): Unit = withContext(Dispatchers.Default) {
        engineMutex.withLock {
            val stream = ensureEngineLocked() ?: return@withContext
            val engine = recognizer ?: return@withContext

            val samples = FloatArray(frame.samples.size) { frame.samples[it] / SHORT_FULL_SCALE }
            stream.acceptWaveform(samples, RecitationAudioFormat.SAMPLE_RATE_HZ)
            while (engine.isReady(stream)) {
                engine.decode(stream)
            }

            val atEndpoint = engine.isEndpoint(stream)
            emitNewlySettledWords(engine.getResult(stream).text, includeLast = atEndpoint)
            if (atEndpoint) {
                engine.reset(stream)
                settledWordCount = 0
            }
        }
    }

    private fun emitNewlySettledWords(text: String, includeLast: Boolean) {
        val currentWords = text.trim().split(WHITESPACE).filter { it.isNotBlank() }
        val settledCount = if (includeLast) currentWords.size else (currentWords.size - 1).coerceAtLeast(0)
        if (settledCount <= settledWordCount) return
        for (index in settledWordCount until settledCount) {
            _words.tryEmit(currentWords[index])
        }
        settledWordCount = settledCount
    }

    override fun reset() {
        stream?.release()
        stream = recognizer?.createStream()
        settledWordCount = 0
    }

    /** Lazily builds the recognizer + stream once the model is downloaded - safe to call
     * repeatedly, a no-op once built. Caller must hold [engineMutex]. */
    private fun ensureEngineLocked(): OnlineStream? {
        stream?.let { return it }
        val modelFile = asrModelRepository.modelFileOrNull() ?: return null
        val tokensFile = asrModelRepository.tokensFileOrNull() ?: return null

        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = RecitationAudioFormat.SAMPLE_RATE_HZ, featureDim = FEATURE_DIM),
            modelConfig = OnlineModelConfig(
                zipformer2Ctc = OnlineZipformer2CtcModelConfig(model = modelFile.absolutePath),
                tokens = tokensFile.absolutePath,
                numThreads = NUM_THREADS,
                provider = "cpu",
            ),
            decodingMethod = "greedy_search",
            enableEndpoint = true,
        )
        val newRecognizer = OnlineRecognizer(assetManager = null, config = config)
        val newStream = newRecognizer.createStream()
        recognizer = newRecognizer
        stream = newStream
        return newStream
    }

    private companion object {
        const val SHORT_FULL_SCALE = 32768.0f
        const val FEATURE_DIM = 80
        const val NUM_THREADS = 2
        val WHITESPACE = Regex("\\s+")
    }
}
