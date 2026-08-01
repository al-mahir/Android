package com.example.mushaf.data.recite.local

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.recite.local.SpeechRecognitionAvailability
import com.example.mushaf.domain.repository.LocalSpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Wraps `android.speech.SpeechRecognizer`, preferring the on-device engine so the spike actually
 * measures what [SpeechRecognitionAvailability.ON_DEVICE] can do — see
 * docs/features/06-taahud-local-recitation-tracking-plan.md §2 Gap D and §5 Phase 2.
 *
 * The public API has no "feed a PCM buffer" entry point; it owns the mic itself via
 * [SpeechRecognizer.startListening]. It also has no continuous-listening mode - a session ends
 * at each pause, so this restarts listening after every result/recoverable error to approximate
 * one, the same shape the iOS build's `AVAudioEngine`-driven continuous task achieves natively.
 */
class AndroidOnDeviceSpeechRecognizer(
    private val context: Context,
) : LocalSpeechRecognizer {

    override fun availability(): SpeechRecognitionAvailability {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            return SpeechRecognitionAvailability.ON_DEVICE
        }
        return if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognitionAvailability.NETWORK_ONLY
        } else {
            SpeechRecognitionAvailability.UNAVAILABLE
        }
    }

    /**
     * Diagnostic only, fire-and-forget: asks the device's speech service which languages it
     * actually supports. `availability()` only confirms *a* recognizer exists, not that it
     * covers Arabic — this is how to tell the two apart when `ERROR_LANGUAGE_NOT_SUPPORTED`
     * shows up, without guessing. Logs asynchronously; never blocks or throws.
     */
    fun logSupportedLanguages() {
        runCatching {
            context.sendOrderedBroadcast(
                Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS),
                null,
                object : BroadcastReceiver() {
                    override fun onReceive(receiverContext: Context, intent: Intent) {
                        val extras = getResultExtras(true)
                        val supported = extras?.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
                        val preferred = extras?.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)
                        Log.i(
                            MushafLog.TAG,
                            "Speech recognizer languages: preferred=$preferred, " +
                                "arabicSupported=${supported?.any { it.startsWith("ar") }}, " +
                                "all=$supported",
                        )
                    }
                },
                null,
                android.app.Activity.RESULT_OK,
                null,
                null,
            )
        }.onFailure { Log.w(MushafLog.TAG, "Could not query recognizer supported languages", it) }
    }

    /**
     * `availability() == ON_DEVICE` only confirms an on-device recognizer exists for *some*
     * language — confirmed on real hardware that this can be true while the on-device catalog
     * has zero Arabic variants (`checkRecognitionSupport` is the API this is actually meant to
     * be verified through, Android 14+ only; [logSupportedLanguages]'s legacy broadcast doesn't
     * get answered by on-device recognizer services). Below API 34, or if the device can't be
     * asked, this assumes *not* supported rather than risking a blind on-device attempt that's
     * already been proven to just fail with `ERROR_LANGUAGE_NOT_SUPPORTED` — [listen] falls
     * through to the network-based recognizer instead, which commonly does cover Arabic.
     */
    private suspend fun onDeviceSupportsArabic(): Boolean {
        if (Build.VERSION.SDK_INT < 34) return false
        return runCatching {
            suspendCancellableCoroutine { continuation ->
                val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                recognizer.checkRecognitionSupport(
                    intent,
                    context.mainExecutor,
                    object : android.speech.RecognitionSupportCallback {
                        override fun onSupportResult(recognitionSupport: android.speech.RecognitionSupport) {
                            val supportsArabic =
                                recognitionSupport.installedOnDeviceLanguages.any { it.startsWith("ar") } ||
                                    recognitionSupport.supportedOnDeviceLanguages.any { it.startsWith("ar") }
                            Log.i(
                                MushafLog.TAG,
                                "On-device recognition support: installed=${recognitionSupport.installedOnDeviceLanguages}, " +
                                    "supported=${recognitionSupport.supportedOnDeviceLanguages}, arabic=$supportsArabic",
                            )
                            recognizer.destroy()
                            if (continuation.isActive) continuation.resume(supportsArabic)
                        }

                        override fun onError(error: Int) {
                            Log.w(MushafLog.TAG, "On-device recognition support check failed: ${errorName(error)}")
                            recognizer.destroy()
                            if (continuation.isActive) continuation.resume(false)
                        }
                    },
                )
                continuation.invokeOnCancellation { recognizer.destroy() }
            }
        }.getOrElse {
            Log.w(MushafLog.TAG, "Could not check on-device recognition support", it)
            false
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun listen(): Flow<String> = callbackFlow {
        logSupportedLanguages()
        val currentAvailability = availability()
        check(currentAvailability != SpeechRecognitionAvailability.UNAVAILABLE) {
            "No speech recognizer available on this device"
        }
        val onDevice = currentAvailability == SpeechRecognitionAvailability.ON_DEVICE && onDeviceSupportsArabic()
        Log.i(MushafLog.TAG, "Local speech recognizer: using ${if (onDevice) "on-device" else "network"} engine")

        val recognizer = if (onDevice) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        // callbackFlow's ProducerScope is a CoroutineScope, but that's only an implicit
        // receiver inside this lambda - the local `fun`s below can't see it through their own
        // scope, so it's captured explicitly here instead.
        val producerScope = this
        var settleJob: Job? = null
        var lastSentWord: String? = null

        fun settledWordsIn(text: String): String? = text.trim().split(WHITESPACE).lastOrNull { it.isNotBlank() }

        fun maybeSend(word: String?, immediate: Boolean) {
            val candidate = word?.trim().orEmpty()
            // No filler-word blocklist here on purpose: many Qur'an words that would look like
            // filler in everyday speech ("من", "في", "على"...) are exactly where a reciter can
            // legitimately start or where a real word falls, and the start-detection use of this
            // stream needs every settled word, not just "interesting-looking" ones.
            if (candidate.length < MIN_WORD_LENGTH) return
            settleJob?.cancel()
            if (immediate) {
                if (candidate != lastSentWord) {
                    lastSentWord = candidate
                    trySend(candidate)
                }
                return
            }
            settleJob = producerScope.launch {
                delay(WORD_SETTLE_DELAY_MS)
                if (candidate != lastSentWord) {
                    lastSentWord = candidate
                    trySend(candidate)
                }
            }
        }

        fun startListening() {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Bare "ar", not "ar-SA": some engines (confirmed: ERROR_LANGUAGE_NOT_SUPPORTED
                // on at least one real device/emulator combo) only register the base language
                // code, not the region-qualified BCP-47 tag.
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, onDevice)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            recognizer.startListening(intent)
        }

        fun restartListening() {
            if (!isClosedForSend) startListening()
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onPartialResults(partialResults: Bundle) {
                val text = partialResults
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                maybeSend(settledWordsIn(text), immediate = false)
            }

            override fun onResults(results: Bundle) {
                val text = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                maybeSend(text?.let(::settledWordsIn), immediate = true)
                lastSentWord = null
                restartListening()
            }

            override fun onError(error: Int) {
                Log.w(MushafLog.TAG, "Local speech recognizer error: ${errorName(error)}")
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    -> restartListening()

                    else -> close(IllegalStateException("Local speech recognizer failed: ${errorName(error)}"))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

        recognizer.setRecognitionListener(listener)
        startListening()

        awaitClose {
            settleJob?.cancel()
            recognizer.stopListening()
            recognizer.destroy()
        }
    }.flowOn(Dispatchers.Main)

    private fun errorName(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "ERROR_TOO_MANY_REQUESTS"
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "ERROR_SERVER_DISCONNECTED"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "ERROR_LANGUAGE_NOT_SUPPORTED"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "ERROR_LANGUAGE_UNAVAILABLE"
        else -> "ERROR($code)"
    }

    private companion object {
        const val WORD_SETTLE_DELAY_MS = 450L
        const val MIN_WORD_LENGTH = 2
        val WHITESPACE = Regex("\\s+")
    }
}
