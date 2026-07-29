package com.example.mushaf.data.tafsir.local

import android.content.Context
import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.DownloadState
import io.ktor.client.HttpClient
import io.ktor.client.statement.readBytes
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File

class TafsirDownloadManager(
    private val context: Context,
    private val client: HttpClient,
) {
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val tafsirsDir = File(context.filesDir, "tafsirs").apply { mkdirs() }

    fun isDownloaded(tafsirKey: String): Boolean {
        return getTafsirFile(tafsirKey).exists()
    }

    fun getTafsirFile(tafsirKey: String): File {
        return File(tafsirsDir, "${tafsirKey}.json.gz")
    }

    fun getDownloadedTafsirKeys(): List<String> {
        val files = tafsirsDir.listFiles() ?: return emptyList()
        return files.filter { it.name.endsWith(".json.gz") }
            .map { it.name.removeSuffix(".json.gz") }
    }

    suspend fun downloadTafsir(tafsirKey: String, url: String) = withContext(Dispatchers.IO) {
        val file = getTafsirFile(tafsirKey)
        if (file.exists()) return@withContext

        try {
            _downloadStates.update { it + (tafsirKey to DownloadState.Downloading(0f)) }
            
            // Download file
            val responseBytes: ByteArray = client.get(url).readBytes()
            
            // Write to temp file first, then rename to avoid corrupted partial downloads
            val tempFile = File(tafsirsDir, "${tafsirKey}.tmp")
            tempFile.writeBytes(responseBytes)
            tempFile.renameTo(file)
            
            _downloadStates.update { it + (tafsirKey to DownloadState.Downloaded) }
            Log.d(MushafLog.TAG, "Successfully downloaded Tafsir $tafsirKey")
        } catch (e: Exception) {
            Log.e(MushafLog.TAG, "Failed to download Tafsir $tafsirKey", e)
            _downloadStates.update { it - tafsirKey } // Remove from downloading state
        }
    }

    fun deleteTafsir(tafsirKey: String) {
        val file = getTafsirFile(tafsirKey)
        if (file.exists()) {
            file.delete()
        }
        _downloadStates.update { it - tafsirKey }
    }
}
