package com.example.mushaf.data.recitation.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mushaf.data.core.di.SEARCH_CLIENT
import com.example.mushaf.data.recitation.local.AyahTimingEntity
import com.example.mushaf.data.recitation.local.DownloadStatusEntity
import com.example.mushaf.data.recitation.local.RecitationDao
import com.example.mushaf.data.recitation.remote.QuranApi
import com.example.mushaf.domain.model.SurahCatalog
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File
import java.io.FileOutputStream

class AudioDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val recitationDao: RecitationDao by inject()
    private val quranApi: QuranApi by inject()
    private val httpClient: HttpClient by inject(named(SEARCH_CLIENT))

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
                    if (isStopped) return@withContext Result.failure()
                    val surahStatusId = "${reciterId}_$s"
                    updateProgress(surahStatusId, reciterId, s, 0, "DOWNLOADING")
                    downloadSurah(reciterId, s, isFullQuran = true, statusId = surahStatusId)
                    val progress = ((s / 114f) * 100).toInt()
                    updateProgress(statusId, reciterId, surahId, progress, "DOWNLOADING")
                }
                updateProgress(statusId, reciterId, surahId, 100, "COMPLETED")
            } else {
                // Download single surah
                downloadSurah(reciterId, surahId, isFullQuran = false, statusId = statusId)
                updateProgress(statusId, reciterId, surahId, 100, "COMPLETED")
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            updateProgress(statusId, reciterId, surahId, 0, "ERROR", e.message)
            Result.failure()
        }
    }

    private fun getReciterBaseUrl(reciterId: Int): String {
        return when (reciterId) {
            7 -> "https://audio.qurancdn.com/Alafasy/mp3/"
            3 -> "https://audio.qurancdn.com/Sudais/mp3/"
            4 -> "https://audio.qurancdn.com/Shatri/mp3/"
            5 -> "https://audio.qurancdn.com/Rifai/mp3/"
            1 -> "https://audio.qurancdn.com/AbdulBaset/Mujawwad/mp3/"
            2 -> "https://audio.qurancdn.com/AbdulBaset/Murattal/mp3/"
            6 -> "https://audio.qurancdn.com/Husary/mp3/"
            12 -> "https://audio.qurancdn.com/Husary/Muallim/mp3/"
            9 -> "https://audio.qurancdn.com/Minshawy/Murattal/mp3/"
            8 -> "https://audio.qurancdn.com/Minshawy/Mujawwad/mp3/"
            10 -> "https://audio.qurancdn.com/Shuraym/mp3/"
            11 -> "https://audio.qurancdn.com/Tablawi/mp3/"
            else -> "https://audio.qurancdn.com/Alafasy/mp3/"
        }
    }

    private suspend fun downloadSurah(reciterId: Int, surahId: Int, isFullQuran: Boolean, statusId: String) {
        val surahCatalogItem = SurahCatalog.all.getOrNull(surahId - 1)
        val totalVerses = surahCatalogItem?.verseCount ?: 7
        val baseUrl = getReciterBaseUrl(reciterId)

        // Try getting API metadata if available, but do not fail if offline/blocked
        val response = try {
            quranApi.getVersesByChapter(surahId, reciterId)
        } catch (e: Exception) {
            null
        }

        val entities = mutableListOf<AyahTimingEntity>()
        val audioDir = File(applicationContext.filesDir, "audio/$reciterId")
        if (!audioDir.exists()) audioDir.mkdirs()

        for (verseNumber in 1..totalVerses) {
            if (isStopped) break

            val verseKey = "$surahId:$verseNumber"
            val apiVerse = response?.verses?.find { it.verseKey == verseKey || it.verseNumber == verseNumber }
            val audio = apiVerse?.audio

            val rawUrl = audio?.url
            val mp3Url = when {
                rawUrl != null && rawUrl.startsWith("//") -> "https:$rawUrl"
                rawUrl != null && (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) -> rawUrl
                rawUrl != null && rawUrl.isNotBlank() -> "https://audio.qurancdn.com/${rawUrl.removePrefix("/")}"
                else -> {
                    val paddedS = surahId.toString().padStart(3, '0')
                    val paddedA = verseNumber.toString().padStart(3, '0')
                    "$baseUrl$paddedS$paddedA.mp3"
                }
            }

            val segments = audio?.segments?.map { rawSegment ->
                if (rawSegment.size >= 4) listOf(rawSegment[0], rawSegment[2], rawSegment[3])
                else if (rawSegment.size == 3) listOf(rawSegment[0], rawSegment[1], rawSegment[2])
                else emptyList()
            }?.filter { it.isNotEmpty() } ?: emptyList()

            val timestampFrom = segments.firstOrNull()?.get(1) ?: 0L
            val timestampTo = segments.lastOrNull()?.get(2) ?: 0L

            // Download MP3
            var localPath: String? = null
            try {
                val mp3Bytes = httpClient.get(mp3Url).readRawBytes()
                val mp3File = File(audioDir, "${surahId}_${verseNumber}.mp3")
                FileOutputStream(mp3File).use { it.write(mp3Bytes) }
                localPath = mp3File.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }

            entities.add(
                AyahTimingEntity(
                    id = "${reciterId}_$verseKey",
                    reciterId = reciterId,
                    verseKey = verseKey,
                    timestampFrom = timestampFrom,
                    timestampTo = timestampTo,
                    audioUrl = mp3Url,
                    localAudioPath = localPath,
                    segmentsJson = Json.encodeToString(segments)
                )
            )

            if (totalVerses > 0) {
                val progress = (((verseNumber) / totalVerses.toFloat()) * 100).toInt()
                updateProgress(statusId, reciterId, surahId, progress, "DOWNLOADING")
            }
        }

        recitationDao.insertAyahTimings(entities)
        updateProgress(statusId, reciterId, surahId, 100, "COMPLETED")
    }

    private suspend fun updateProgress(id: String, reciterId: Int, surahId: Int?, progress: Int, state: String, error: String? = null) {
        recitationDao.insertOrUpdateDownloadStatus(
            DownloadStatusEntity(
                id = id,
                reciterId = reciterId,
                surahId = surahId,
                progress = progress,
                state = state,
                totalSizeBytes = 0L,
                downloadedBytes = 0L,
                errorMessage = error
            )
        )
    }

    companion object {
        const val KEY_RECITER_ID = "RECITER_ID"
        const val KEY_SURAH_ID = "SURAH_ID"
    }
}
