package com.example.mushaf.data.repository

import com.example.mushaf.data.recitation.RecitationDataSource
import com.example.mushaf.data.recitation.RecitationMapper
import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.domain.repository.RecitationRepository
import com.iti.domain.core.Result
import com.iti.domain.core.DomainError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull

class RecitationRepositoryImpl(
    private val dataSource: RecitationDataSource,
    private val dao: com.example.mushaf.data.recitation.local.RecitationDao,
    private val mushafRepository: com.example.mushaf.domain.repository.MushafRepository,
    private val context: android.content.Context
) : RecitationRepository {

    override fun getReciters(): Flow<Result<List<Reciter>>> {
        return dataSource.observeReciters()
            .map { dtoList ->
                val reciters = dtoList.map { RecitationMapper.toDomain(it) }
                Result.Success(reciters) as Result<List<Reciter>>
            }
            .catch { e ->
                emit(Result.Error(DomainError.NetworkError(e)))
            }
    }

    override fun getTimingsForPage(reciterId: Int, pageNumber: Int): Flow<Result<List<AyahTiming>>> {
        return kotlinx.coroutines.flow.flow {
            // Get verse keys for this page from the local layout DB
            val mushafPage = mushafRepository.getPage(pageNumber).firstOrNull()
            val verseKeys = mushafPage?.lines?.flatMap { it.words }
                ?.map { word -> word.id.substringBeforeLast(":") } // 1:1:1 -> 1:1
                ?.distinct()
                ?: emptyList()
            
            // Try fetching from remote first, but if it fails, fallback to local
            val remoteTimingsFlow = dataSource.observeTimingsForPage(reciterId, pageNumber).catch { 
                emit(emptyList()) 
            }
            
            remoteTimingsFlow.collect { remoteList ->
                val localEntities = dao.getAyahTimings(reciterId, verseKeys)
                
                val finalTimings = if (remoteList.isEmpty() && localEntities.isNotEmpty()) {
                    // Offline fallback: Use fully local entities
                    localEntities.map { local ->
                        val (surah, ayah) = local.verseKey.split(":").map { it.toInt() }
                        val segmentsList = kotlinx.serialization.json.Json.decodeFromString<List<List<Long>>>(local.segmentsJson)
                        val wordTimings = segmentsList.map { segmentArray ->
                            com.example.mushaf.domain.model.WordTiming(
                                wordIndex = segmentArray[0].toInt(),
                                startMs = segmentArray[1],
                                endMs = segmentArray[2]
                            )
                        }
                        AyahTiming(
                            surahNumber = surah,
                            ayahNumber = ayah,
                            timestampFrom = local.timestampFrom,
                            timestampTo = local.timestampTo,
                            audioUrl = local.localAudioPath ?: local.audioUrl,
                            wordTimings = wordTimings
                        )
                    }
                } else {
                    // Online: Map remote and overlay with local audio path if downloaded
                    remoteList.map { dto ->
                        val localMatch = localEntities.find { it.verseKey == dto.verseKey }
                        val domainTiming = RecitationMapper.toDomain(dto)
                        if (localMatch?.localAudioPath != null && java.io.File(localMatch.localAudioPath).exists()) {
                            domainTiming.copy(audioUrl = localMatch.localAudioPath)
                        } else {
                            domainTiming
                        }
                    }
                }
                
                if (finalTimings.isNotEmpty()) {
                    emit(Result.Success(finalTimings) as Result<List<AyahTiming>>)
                } else {
                    emit(Result.Error(DomainError.NetworkError(Exception("Offline and no local data"))))
                }
            }
        }.catch { e ->
            emit(Result.Error(DomainError.NetworkError(e)))
        }
    }

    override suspend fun downloadRecitation(reciterId: Int, surahNumber: Int?) {
        val data = androidx.work.Data.Builder()
            .putInt(com.example.mushaf.data.recitation.download.AudioDownloadWorker.KEY_RECITER_ID, reciterId)
            .putInt(com.example.mushaf.data.recitation.download.AudioDownloadWorker.KEY_SURAH_ID, surahNumber ?: -1)
            .build()
            
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
            
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.mushaf.data.recitation.download.AudioDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()
            
        androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
    }

    override fun observeDownloadProgress(reciterId: Int): Flow<List<com.example.mushaf.domain.model.DownloadStatus>> {
        return dao.observeAllDownloadStatusesForReciter(reciterId).map { entities ->
            entities.map { entity ->
                com.example.mushaf.domain.model.DownloadStatus(
                    id = entity.id,
                    reciterId = entity.reciterId,
                    surahId = entity.surahId,
                    progress = entity.progress,
                    state = entity.state,
                    errorMessage = entity.errorMessage
                )
            }
        }
    }
}
