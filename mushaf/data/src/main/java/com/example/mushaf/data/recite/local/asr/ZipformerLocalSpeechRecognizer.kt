package com.example.mushaf.data.recite.local.asr

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import com.example.mushaf.domain.model.recite.SpeechGateConfig
import com.example.mushaf.domain.model.recite.local.AsrModelState
import com.example.mushaf.domain.model.recite.local.LocalTranscript
import com.example.mushaf.domain.repository.LocalSpeechRecognizer
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
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
 *
 * Emits the running transcript after every decode - roughly every 100ms frame - rather than
 * waiting for anything to "settle". Two findings forced that shape, both confirmed against the
 * model's own `tokens.txt` and sherpa-onnx's compiled defaults rather than guessed:
 *
 * 1. **The vocabulary has no word delimiter.** All 251 symbols are bare Arabic letters or
 *    letter+ḥaraka pairs; there is no space and no SentencePiece `▁`. An earlier version split
 *    `getResult().text` on whitespace and emitted everything but the last token, which meant the
 *    token count was permanently 1 and *nothing was ever emitted* except at an endpoint.
 * 2. **Endpointing can barely fire here.** sherpa-onnx's defaults need 1.4s (rule 2) or 2.4s
 *    (rule 1) of trailing silence, but [SpeechGateConfig.hangoverFrames] closes the gate after
 *    600ms and then stops feeding frames entirely, so only rule 3 - a 20 *second* utterance cap -
 *    could ever trigger. [ENDPOINT_TRAILING_SILENCE_SECONDS] is set inside the hangover window so
 *    a real pause resets the decoder, and nothing downstream depends on that happening.
 *
 * Segmenting the phoneme stream into words is
 * [com.example.mushaf.domain.model.recite.local.PhonemeCursorTracker]'s job, against the text the
 * reciter is expected to be reading - the model cannot do it and never could.
 */
class ZipformerLocalSpeechRecognizer(
    private val asrModelRepository: AsrModelRepositoryImpl,
) : LocalSpeechRecognizer {

    private val _transcript = MutableSharedFlow<LocalTranscript>(extraBufferCapacity = 64)
    override val transcript: SharedFlow<LocalTranscript> = _transcript.asSharedFlow()

    override val isAvailable: Boolean
        get() = asrModelRepository.state.value is AsrModelState.Ready

    private val engineMutex = Mutex()
    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private var emittedLength = 0

    override suspend fun accept(frame: AudioFrame): Unit = withContext(Dispatchers.Default) {
        engineMutex.withLock {
            val stream = ensureEngineLocked() ?: return@withContext
            val engine = recognizer ?: return@withContext

            val samples = FloatArray(frame.samples.size) { frame.samples[it] / SHORT_FULL_SCALE }
            stream.acceptWaveform(samples, RecitationAudioFormat.SAMPLE_RATE_HZ)
            while (engine.isReady(stream)) {
                engine.decode(stream)
            }

            val text = engine.getResult(stream).text
            val atEndpoint = engine.isEndpoint(stream)

            // Re-emitting an unchanged transcript would make every consumer redo the same
            // alignment work 10 times a second for nothing; a shrinking one can't happen without
            // a reset, which is handled below.
            if (text.length != emittedLength || atEndpoint) {
                _transcript.tryEmit(LocalTranscript(phonemes = text, isFinal = atEndpoint))
                emittedLength = text.length
            }

            if (atEndpoint) {
                engine.reset(stream)
                emittedLength = 0
            }
        }
    }

    override suspend fun reset() {
        engineMutex.withLock {
            stream?.release()
            stream = recognizer?.createStream()
            emittedLength = 0
        }
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
            endpointConfig = EndpointConfig(
                rule1 = EndpointRule(false, ENDPOINT_TRAILING_SILENCE_SECONDS, 0f),
                rule2 = EndpointRule(true, ENDPOINT_TRAILING_SILENCE_SECONDS, 0f),
                rule3 = EndpointRule(false, 0f, MAX_UTTERANCE_SECONDS),
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

        /** Comfortably inside the speech gate's 600ms hangover, so a genuine pause resets the
         * decoder instead of letting one "utterance" run until the 20s cap. */
        const val ENDPOINT_TRAILING_SILENCE_SECONDS = 0.4f

        /** Bounds how long a single decode context can grow; a shorter cap than sherpa-onnx's
         * default would cost accuracy, a longer one memory, and neither matters much now that
         * nothing waits for an endpoint to emit. */
        const val MAX_UTTERANCE_SECONDS = 20f
    }
}
