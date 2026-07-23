package com.example.mushaf.data.recitation.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mushaf.data.recitation.local.AyahTimingEntity
import com.example.mushaf.data.recitation.local.DownloadStatusEntity
import com.example.mushaf.data.recitation.local.RecitationDao
import com.example.mushaf.data.recitation.remote.QuranApi
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream

class AudioDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val recitationDao: RecitationDao by inject()
    private val quranApi: QuranApi by inject()
    private val httpClient: HttpClient by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val reciterId = inputData.getInt(KEY_RECITER_ID, -1)
        val surahId = inputData.getInt(KEY_SURAH_ID, -1)

        if (reciterId == -1) return@withContext Result.failure()

        val isFullQuran = surahId == -1
        val statusId = if (isFullQuran) "${reciterId}_full" else "${reciterId}_$surahId"

        try {
            updateProgress(statusId, reciterId, surahId, 0, "DOWNLOADING")

            if (isFullQuran) {
                // Download all 114 surahs
                for (s in 1..114) {
                    downloadSurah(reciterId, s)
                    val progress = ((s / 114f) * 100).toInt()
                    updateProgress(statusId, reciterId, surahId, progress, "DOWNLOADING")
                }
            } else {
                // Download single surah
                downloadSurah(reciterId, surahId)
                updateProgress(statusId, reciterId, surahId, 100, "COMPLETED")
            }

            if (isFullQuran) {
                updateProgress(statusId, reciterId, surahId, 100, "COMPLETED")
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            updateProgress(statusId, reciterId, surahId, 0, "ERROR", e.message)
            Result.failure()
        }
    }

    private suspend fun downloadSurah(reciterId: Int, surahId: Int) {
        // QuranApi fetches by chapter (surah)
        val response = quranApi.getVersesByChapter(surahId, reciterId)
        
        val entities = mutableListOf<AyahTimingEntity>()
        val audioDir = File(applicationContext.filesDir, "audio/$reciterId")
        if (!audioDir.exists()) audioDir.mkdirs()

        for (verse in response.verses) {
            val audio = verse.audio ?: continue
            val segments = audio.segments.map { rawSegment ->
                if (rawSegment.size >= 4) listOf(rawSegment[0], rawSegment[2], rawSegment[3])
                else if (rawSegment.size == 3) listOf(rawSegment[0], rawSegment[1], rawSegment[2])
                else emptyList()
            }.filter { it.isNotEmpty() }

            val timestampFrom = segments.firstOrNull()?.get(1) ?: 0L
            val timestampTo = segments.lastOrNull()?.get(2) ?: 0L

            // Download MP3
            var localPath: String? = null
            if (!audio.url.isNullOrBlank()) {
                val mp3Url = if (audio.url.startsWith("//")) "https:${audio.url}" else audio.url
                val mp3Bytes = httpClient.get(mp3Url).readBytes()
                val mp3File = File(audioDir, "${verse.verseKey}.mp3")
                FileOutputStream(mp3File).use { it.write(mp3Bytes) }
                localPath = mp3File.absolutePath
            }

            entities.add(
                AyahTimingEntity(
                    id = "${reciterId}_${verse.verseKey}",
                    reciterId = reciterId,
                    verseKey = verse.verseKey,
                    timestampFrom = timestampFrom,
                    timestampTo = timestampTo,
                    audioUrl = audio.url.orEmpty(),
                    localAudioPath = localPath,
                    segmentsJson = Json.encodeToString(segments)
                )
            )
        }

        recitationDao.insertAyahTimings(entities)
    }

    private suspend fun updateProgress(id: String, reciterId: Int, surahId: Int?, progress: Int, state: String, error: String? = null) {
        recitationDao.insertOrUpdateDownloadStatus(
            DownloadStatusEntity(
                id = id,
                reciterId = reciterId,
                surahId = surahId,
                progress = progress,
                state = state,
                totalSizeBytes = 0L, // Add logic if needed
                downloadedBytes = 0L, // Add logic if needed
                errorMessage = error
            )
        )
    }

    companion object {
        const val KEY_RECITER_ID = "RECITER_ID"
        const val KEY_SURAH_ID = "SURAH_ID"
    }
}
