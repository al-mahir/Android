package com.example.mushaf.data.tafsir.local

import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.TafsirResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.zip.GZIPInputStream

@Serializable
data class TafsirJsonDto(
    val surah: Int,
    val ayah: Int,
    val text: String
)

class TafsirLocalJsonDataSource(
    private val downloadManager: TafsirDownloadManager,
) {
    private val mutex = Mutex()
    private var cachedTafsirKey: String? = null
    private var cachedTafsirMap: Map<String, String> = emptyMap()

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadTafsirIfNeeded(tafsirKey: String) {
        if (cachedTafsirKey == tafsirKey) return

        mutex.withLock {
            if (cachedTafsirKey == tafsirKey) return@withLock
            val file = downloadManager.getTafsirFile(tafsirKey)
            if (!file.exists()) {
                cachedTafsirKey = null
                cachedTafsirMap = emptyMap()
                return@withLock
            }

            try {
                Log.d(MushafLog.TAG, "Loading offline JSON tafsir $tafsirKey into memory...")
                val map = mutableMapOf<String, String>()
                GZIPInputStream(file.inputStream()).use { inputStream ->
                    val dtoList = Json { ignoreUnknownKeys = true }.decodeFromStream<List<TafsirJsonDto>>(inputStream)
                    dtoList.forEach { dto ->
                        map["${dto.surah}:${dto.ayah}"] = dto.text
                    }
                }
                cachedTafsirMap = map
                cachedTafsirKey = tafsirKey
                Log.d(MushafLog.TAG, "Successfully loaded offline JSON tafsir $tafsirKey (${map.size} ayahs)")
            } catch (e: Exception) {
                Log.e(MushafLog.TAG, "Failed to load offline JSON tafsir $tafsirKey", e)
                cachedTafsirKey = null
                cachedTafsirMap = emptyMap()
            }
        }
    }

    suspend fun getTafsirForAyah(tafsirKey: String, surah: Int, ayah: Int): TafsirResult? = withContext(Dispatchers.IO) {
        if (!downloadManager.isDownloaded(tafsirKey)) return@withContext null

        loadTafsirIfNeeded(tafsirKey)
        val text = cachedTafsirMap["$surah:$ayah"] ?: return@withContext null

        TafsirResult(
            surahNumber = surah,
            ayahNumber = ayah,
            tafsirText = text
        )
    }
}
