package com.example.mushaf.data.tafsir.local

import android.content.Context
import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.DownloadFailure
import com.example.mushaf.domain.model.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages downloading, caching, and deleting Tafsir JSON database files.
 *
 * Uses plain [java.net.HttpURLConnection] for downloading so that:
 *  - No Ktor base-URL prefix or auth headers are injected.
 *  - Downloads from any external URL work correctly (e.g. GitHub releases).
 *  - Progress reporting is possible (via Content-Length).
 */
class TafsirDownloadManager(
    private val context: Context,
) {
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val tafsirsDir = File(context.filesDir, "tafsirs").apply { mkdirs() }

    fun isDownloaded(tafsirKey: String): Boolean = getTafsirFile(tafsirKey).exists()

    fun getTafsirFile(tafsirKey: String): File = File(tafsirsDir, "$tafsirKey.json.gz")

    fun getDownloadedTafsirKeys(): List<String> =
        (tafsirsDir.listFiles() ?: return emptyList())
            .filter { it.name.endsWith(".json.gz") }
            .map { it.name.removeSuffix(".json.gz") }

    suspend fun downloadTafsir(tafsirKey: String, url: String) = withContext(Dispatchers.IO) {
        val destFile = getTafsirFile(tafsirKey)
        if (destFile.exists()) {
            // Already downloaded – surface the correct state in case it was cleared.
            _downloadStates.update { it + (tafsirKey to DownloadState.Downloaded) }
            return@withContext
        }

        _downloadStates.update { it + (tafsirKey to DownloadState.Downloading(0f)) }
        val tempFile = File(tafsirsDir, "$tafsirKey.tmp")

        try {
            var connection: HttpURLConnection? = null
            try {
                // Follow up to 5 redirects manually so we handle GitHub / CDN redirects.
                var currentUrl = url
                var redirects = 0
                while (redirects < MAX_REDIRECTS) {
                    connection = URL(currentUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = CONNECT_TIMEOUT_MS
                    connection.readTimeout = READ_TIMEOUT_MS
                    connection.instanceFollowRedirects = false // handle manually for logging
                    connection.connect()

                    val code = connection.responseCode
                    Log.d(MushafLog.TAG, "downloadTafsir $tafsirKey → $currentUrl → HTTP $code")

                    if (code in 300..399) {
                        val location = connection.getHeaderField("Location")
                            ?: throw IOException("Redirect without Location header")
                        connection.disconnect()
                        currentUrl = location
                        redirects++
                        continue
                    }

                    if (code != HttpURLConnection.HTTP_OK) {
                        throw IOException("HTTP $code for URL $currentUrl")
                    }
                    break
                }
                if (redirects == MAX_REDIRECTS) throw IOException("Too many redirects for $url")

                val totalBytes = connection!!.contentLengthLong
                var downloadedBytes = 0L

                tempFile.outputStream().buffered().use { out ->
                    connection.inputStream.buffered().use { input ->
                        val buf = ByteArray(BUFFER_SIZE)
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            out.write(buf, 0, n)
                            downloadedBytes += n
                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes
                                _downloadStates.update { it + (tafsirKey to DownloadState.Downloading(progress)) }
                            }
                        }
                    }
                }
            } finally {
                connection?.disconnect()
            }

            // Atomic rename to prevent partially-written files from being treated as valid.
            val renamed = tempFile.renameTo(destFile)
            if (!renamed) {
                // renameTo can fail across file systems; fall back to copy + delete.
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }

            _downloadStates.update { it + (tafsirKey to DownloadState.Downloaded) }
            Log.d(MushafLog.TAG, "Successfully downloaded Tafsir $tafsirKey (${destFile.length()} bytes)")

        } catch (e: Exception) {
            Log.e(MushafLog.TAG, "Failed to download Tafsir $tafsirKey from $url", e)
            tempFile.delete()
            val failure = when (e) {
                is IOException -> DownloadFailure.NO_CONNECTION
                else -> DownloadFailure.UNKNOWN
            }
            _downloadStates.update { it + (tafsirKey to DownloadState.Failed(failure)) }
        }
    }

    fun resetFailedState(tafsirKey: String) {
        if (_downloadStates.value[tafsirKey] is DownloadState.Failed) {
            _downloadStates.update { it - tafsirKey }
        }
    }

    fun deleteTafsir(tafsirKey: String) {
        getTafsirFile(tafsirKey).delete()
        _downloadStates.update { it - tafsirKey }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 60_000
        const val BUFFER_SIZE = 64 * 1024 // 64 KB
        const val MAX_REDIRECTS = 5
    }
}
