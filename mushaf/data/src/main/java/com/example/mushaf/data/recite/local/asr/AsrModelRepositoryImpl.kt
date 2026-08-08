package com.example.mushaf.data.recite.local.asr

import android.content.Context
import android.util.Log
import com.example.mushaf.data.BuildConfig
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.recite.local.AsrModelState
import com.example.mushaf.domain.repository.AsrModelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the on-device streaming ASR model (`Muno459/zipformer_p-quran`'s int8 build, ~73MB)
 * from Hugging Face on first use, straight into app-private storage - no hosting infrastructure
 * of our own needed. The repo is gated (confirmed on a real device: an unauthenticated request
 * gets HTTP 401 `GatedRepo`), so requests to huggingface.co itself carry a bearer token from
 * `local.properties` (`almahir.hfToken`, never checked into version control - see this module's
 * build.gradle.kts). Same `HttpURLConnection` redirect-handling pattern as
 * [com.example.mushaf.data.tafsir.local.TafsirDownloadManager], just triggered transparently
 * rather than by an explicit user tap - see [AsrModelState]'s doc for why this isn't a
 * Downloads-catalogue item.
 */
class AsrModelRepositoryImpl(
    context: Context,
) : AsrModelRepository {

    private val _state = MutableStateFlow<AsrModelState>(AsrModelState.NotDownloaded)
    override val state: StateFlow<AsrModelState> = _state.asStateFlow()

    private val modelDir = File(context.filesDir, "asr_models/quran_phoneme_zipformer").apply { mkdirs() }
    private val modelFile = File(modelDir, MODEL_FILE_NAME)
    private val tokensFile = File(modelDir, TOKENS_FILE_NAME)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null

    init {
        if (modelFile.exists() && tokensFile.exists()) {
            _state.value = AsrModelState.Ready
        }
    }

    /** The model file, once [state] is [AsrModelState.Ready] - null otherwise. */
    fun modelFileOrNull(): File? = modelFile.takeIf { state.value is AsrModelState.Ready }

    /** The tokens file, once [state] is [AsrModelState.Ready] - null otherwise. */
    fun tokensFileOrNull(): File? = tokensFile.takeIf { state.value is AsrModelState.Ready }

    override fun ensureAvailable() {
        if (_state.value is AsrModelState.Ready) return
        if (downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            runCatching { downloadBoth() }
                .onSuccess { _state.value = AsrModelState.Ready }
                .onFailure { error ->
                    Log.w(MushafLog.TAG, "ASR model download failed", error)
                    _state.value = AsrModelState.Failed(error.message)
                }
        }
    }

    private suspend fun downloadBoth() = withContext(Dispatchers.IO) {
        _state.value = AsrModelState.Downloading(0f)
        // The model is ~28,000x the size of tokens.txt - reporting progress purely off the
        // model download and treating tokens.txt as a negligible tail step is accurate enough
        // without the complexity of tracking two byte counters against a shared total.
        downloadTo(MODEL_URL, modelFile) { progress -> _state.value = AsrModelState.Downloading(progress) }
        downloadTo(TOKENS_URL, tokensFile) { }
    }

    private fun downloadTo(url: String, destination: File, onProgress: (Float) -> Unit) {
        if (destination.exists()) return
        val tempFile = File(destination.parentFile, "${destination.name}.tmp")

        var connection: HttpURLConnection? = null
        try {
            var currentUrl = url
            var redirects = 0
            while (redirects < MAX_REDIRECTS) {
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = false
                    // Muno459/zipformer_p-quran is a gated HF repo - only huggingface.co itself
                    // checks this; the CDN URL it redirects to is presigned and neither needs
                    // nor expects it, so this is deliberately NOT forwarded across redirects.
                    if (currentUrl.startsWith(HF_HOST) && BuildConfig.HF_ACCESS_TOKEN.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer ${BuildConfig.HF_ACCESS_TOKEN}")
                    }
                    connect()
                }
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Redirect without Location header for $currentUrl")
                    connection.disconnect()
                    currentUrl = location
                    redirects++
                    continue
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP $code for $currentUrl")
                }
                break
            }
            if (redirects == MAX_REDIRECTS) throw IOException("Too many redirects for $url")

            val totalBytes = connection!!.contentLengthLong
            var downloadedBytes = 0L
            tempFile.outputStream().buffered().use { out ->
                connection.inputStream.buffered().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) onProgress(downloadedBytes.toFloat() / totalBytes)
                    }
                }
            }

            val renamed = tempFile.renameTo(destination)
            if (!renamed) {
                tempFile.copyTo(destination, overwrite = true)
                tempFile.delete()
            }
        } finally {
            connection?.disconnect()
            tempFile.delete()
        }
    }

    private companion object {
        const val MODEL_FILE_NAME = "quran_phoneme_zipformer.int8.onnx"
        const val TOKENS_FILE_NAME = "tokens.txt"
        const val MODEL_URL =
            "https://huggingface.co/Muno459/zipformer_p-quran/resolve/main/$MODEL_FILE_NAME"
        const val TOKENS_URL =
            "https://huggingface.co/Muno459/zipformer_p-quran/resolve/main/$TOKENS_FILE_NAME"
        const val HF_HOST = "https://huggingface.co/"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 60_000
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_REDIRECTS = 5
    }
}
