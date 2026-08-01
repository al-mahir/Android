package com.example.mushaf.data.download

import com.example.mushaf.data.recitation.local.RecitationDao
import com.example.mushaf.domain.model.DownloadState
import com.example.mushaf.domain.model.DownloadableResource
import com.example.mushaf.domain.model.LocalizedText
import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.domain.repository.DownloadableResourceRepository
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.repository.RecitationRepository
import com.iti.domain.core.Result
import com.iti.domain.core.resultOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class DownloadableResourceRepositoryImpl(
    private val recitationRepository: RecitationRepository,
    private val mushafRepository: MushafRepository,
    private val recitationDao: RecitationDao,
) : DownloadableResourceRepository {

    override fun observeResources(kind: ResourceKind): Flow<Result<List<DownloadableResource>>> {
        return when (kind) {
            ResourceKind.RECITER -> observeReciters()
            ResourceKind.TAFSEER -> observeTafsir()
            ResourceKind.TRANSLATION -> observeTranslations()
        }
    }

    private fun observeReciters(): Flow<Result<List<DownloadableResource>>> {
        return recitationRepository.getReciters().map { recitersResult ->
            when (recitersResult) {
                is Result.Success -> {
                    val resources = recitersResult.data.map { reciter ->
                        val localTimings = recitationDao.getAllTimingsForReciter(reciter.id)
                        val downloadedCount = localTimings.count { it.localAudioPath != null && java.io.File(it.localAudioPath).exists() }
                        val state = if (downloadedCount >= 6236) {
                            DownloadState.Downloaded
                        } else if (downloadedCount > 0) {
                            DownloadState.Downloading(downloadedCount.toFloat() / 6236f)
                        } else {
                            DownloadState.NotDownloaded
                        }

                        DownloadableResource(
                            id = reciter.id.toString(),
                            kind = ResourceKind.RECITER,
                            name = LocalizedText(arabic = reciter.nameArabic, english = reciter.name),
                            subtitle = LocalizedText(arabic = reciter.style.name, english = reciter.style.name),
                            sizeBytes = 750L * 1024L * 1024L,
                            state = state,
                        )
                    }
                    Result.Success(resources)
                }
                is Result.Error -> Result.Error(recitersResult.error)
            }
        }
    }

    private fun observeTafsir(): Flow<Result<List<DownloadableResource>>> {
        return mushafRepository.observeAvailableTafsirBooks().map { books ->
            val resources = books.map { book ->
                DownloadableResource(
                    id = book.tafsirKey,
                    kind = ResourceKind.TAFSEER,
                    name = LocalizedText(arabic = book.displayName, english = book.displayName),
                    subtitle = LocalizedText(arabic = book.languageName, english = book.languageName),
                    sizeBytes = book.fileSizeBytes,
                    state = book.state,
                )
            }
            Result.Success(resources)
        }
    }

    private fun observeTranslations(): Flow<Result<List<DownloadableResource>>> {
        return flowOf(Result.Success(TRANSLATIONS_CATALOGUE))
    }

    override suspend fun startDownload(id: String): Result<Unit> = resultOf {
        val reciterId = id.toIntOrNull()
        if (reciterId != null) {
            recitationRepository.downloadRecitation(reciterId, null)
        } else {
            val tafsirBooks = mushafRepository.getAvailableTafsirBooks()
            val book = tafsirBooks.firstOrNull { it.tafsirKey == id }
            if (book != null) {
                mushafRepository.downloadTafsirBook(book.tafsirKey, book.downloadUrl)
            }
        }
    }

    override suspend fun cancelDownload(id: String): Result<Unit> = resultOf {
        val reciterId = id.toIntOrNull()
        if (reciterId != null) {
            recitationRepository.cancelDownloadRecitation(reciterId, null)
        }
    }

    override suspend fun deleteDownload(id: String): Result<Unit> = resultOf {
        val reciterId = id.toIntOrNull()
        if (reciterId != null) {
            recitationRepository.cancelDownloadRecitation(reciterId, null)
            recitationDao.deleteAllTimingsForReciter(reciterId)
        } else {
            mushafRepository.deleteTafsirBook(id)
        }
    }

    private companion object {
        val TRANSLATIONS_CATALOGUE = listOf(
            DownloadableResource(
                id = "translation_en_sahih",
                kind = ResourceKind.TRANSLATION,
                name = LocalizedText("صحيح انترناشونال (إنجليزي)", "Sahih International (English)"),
                sizeBytes = 8 * 1024L * 1024L,
            ),
            DownloadableResource(
                id = "translation_fr_hamidullah",
                kind = ResourceKind.TRANSLATION,
                name = LocalizedText("حميد الله (فرنسي)", "Hamidullah (French)"),
                sizeBytes = 9 * 1024L * 1024L,
            ),
            DownloadableResource(
                id = "translation_ur_junagarhi",
                kind = ResourceKind.TRANSLATION,
                name = LocalizedText("جوناكرهي (أردو)", "Junagarhi (Urdu)"),
                sizeBytes = 10 * 1024L * 1024L,
            ),
            DownloadableResource(
                id = "translation_id_affairs",
                kind = ResourceKind.TRANSLATION,
                name = LocalizedText("الشؤون الإسلامية (إندونيسي)", "Islamic Affairs (Indonesian)"),
                sizeBytes = 7 * 1024L * 1024L,
            ),
        )
    }
}
