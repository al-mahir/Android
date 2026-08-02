package com.example.mushaf.data.repository

import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.core.error.toDomainError
import com.example.mushaf.data.db.MushafAssetDataSource
import com.example.mushaf.data.db.QuranMetadataDataSource
import com.example.mushaf.data.db.QuranTextDataSource
import com.example.mushaf.data.mapper.MushafMapper
import com.example.mushaf.data.tafsir.remote.TafsirRemoteDataSource
import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.Surah
import com.example.mushaf.domain.model.TafsirBook
import com.example.mushaf.domain.model.TafsirResult
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.local.LocalWordEntry
import com.example.mushaf.domain.repository.LocalWordCorpusRepository
import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result
import com.iti.domain.core.asResult
import com.iti.domain.core.resultOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import com.example.mushaf.data.search.remote.SemanticSearchRemoteDataSource

import com.example.mushaf.data.db.TafsirDataSource

import com.example.mushaf.data.tafsir.local.TafsirDownloadManager
import com.example.mushaf.data.tafsir.local.TafsirLocalJsonDataSource

class MushafRepositoryImpl(
    private val dataSource: MushafAssetDataSource,
    private val metadataDataSource: QuranMetadataDataSource,
    private val textDataSource: QuranTextDataSource,
    private val tafsirDataSource: TafsirDataSource,
    private val semanticSearchDataSource: SemanticSearchRemoteDataSource? = null,
    private val tafsirRemoteDataSource: TafsirRemoteDataSource? = null,
    private val tafsirDownloadManager: TafsirDownloadManager? = null,
    private val tafsirLocalJsonDataSource: TafsirLocalJsonDataSource? = null,
) : MushafRepository, LocalWordCorpusRepository {

    private val mapError: (Throwable) -> com.iti.domain.core.DomainError = { it.toDomainError() }

    override fun getPage(pageNumber: Int): Flow<Result<MushafPage>> = flow {
        Log.d(MushafLog.TAG, "getPage($pageNumber) requested")
        val lineRows = dataSource.getLinesForPage(pageNumber)
        val wordRows = dataSource.getWordsForPage(pageNumber)
        val page = MushafMapper.toDomain(pageNumber, lineRows, wordRows)
        Log.d(
            MushafLog.TAG,
            "getPage($pageNumber) mapped: ${page.lines.size} lines, " +
                "${page.lines.sumOf { it.words.size }} words",
        )
        emit(page)
    }.asResult(mapError)

    override suspend fun getPageCount(): Result<Int> = resultOf(mapError) { dataSource.getPageCount() }

    override suspend fun searchSurah(query: String): Result<List<Surah>> = resultOf(mapError) {
        metadataDataSource.searchSurah(query)
    }

    override suspend fun searchJuz(query: String): Result<List<Juz>> = resultOf(mapError) {
        metadataDataSource.searchJuz(query)
    }

    override suspend fun searchHizb(query: String): Result<List<Hizb>> = resultOf(mapError) {
        metadataDataSource.searchHizb(query)
    }

    override suspend fun searchPage(query: String): Result<List<Int>> = resultOf(mapError) {
        dataSource.searchPage(query)
    }

    override suspend fun searchAyah(query: String, limit: Int, offset: Int): Result<List<AyahSearchResult>> =
        resultOf(mapError) {
            val rawResults = textDataSource.searchAyahs(query, limit, offset)
            rawResults.map { raw ->
                val surah = metadataDataSource.getSurah(raw.surahNumber)
                AyahSearchResult(
                    surahNumber = raw.surahNumber,
                    ayahNumber = raw.ayahNumber,
                    ayahText = raw.text,
                    surahNameArabic = surah?.nameAr ?: "",
                    surahNameEnglish = surah?.nameEn ?: ""
                )
            }
        }

    override suspend fun searchAyahByMeaning(
        query: String,
        mode: String,
        hyde: Boolean,
        limit: Int
    ): Result<List<AyahSearchResult>> = resultOf(mapError) {
        val semanticDataSource = semanticSearchDataSource ?: return@resultOf emptyList()
        val response = semanticDataSource.searchByMeaning(query, mode, hyde, limit)
        response.hits.map { hit ->
            val surah = metadataDataSource.getSurah(hit.sura)
            AyahSearchResult(
                surahNumber = hit.sura,
                ayahNumber = hit.aya,
                ayahText = hit.textUthmani,
                surahNameArabic = surah?.nameAr ?: "",
                surahNameEnglish = surah?.nameEn ?: "",
                translation = hit.translation,
                score = hit.score,
                hydeUsed = response.hydeUsed
            )
        }
    }

    override suspend fun getSurahStartingPage(surahNumber: Int): Result<Int?> = resultOf(mapError) {
        dataSource.getSurahStartingPage(surahNumber)
    }

    override suspend fun getAyahPage(surahNumber: Int, ayahNumber: Int): Result<Int?> = resultOf(mapError) {
        dataSource.getAyahPage(surahNumber, ayahNumber)
    }

    override suspend fun getAyahText(surahNumber: Int, ayahNumber: Int): Result<String?> = resultOf(mapError) {
        textDataSource.getVerseText(surahNumber, ayahNumber)
    }

    override suspend fun getJuzStartingPage(juzNumber: Int): Result<Int?> = resultOf(mapError) {
        val juzPages = intArrayOf(
            1, 22, 42, 62, 82, 102, 122, 142, 162, 182,
            202, 222, 242, 262, 282, 302, 322, 342, 362, 382,
            402, 422, 442, 462, 482, 502, 522, 542, 562, 582
        )
        if (juzNumber in 1..30) juzPages[juzNumber - 1] else null
    }

    override suspend fun getTafsirForAyah(surah: Int, ayah: Int): Result<TafsirResult?> = resultOf(mapError) {
        tafsirDataSource.getTafsirForAyah(surah, ayah)?.let { raw ->
            val surahMeta = metadataDataSource.getSurah(raw.surahNumber)
            raw.copy(
                surahNameArabic = surahMeta?.nameAr ?: "",
                surahNameEnglish = surahMeta?.nameEn ?: ""
            )
        }
    }

    override suspend fun getTafsirFromApi(
        surah: Int,
        ayah: Int,
        lang: String,
        tafsirKey: String,
    ): TafsirResult? {
        val remote = tafsirRemoteDataSource ?: return null
        val result = remote.getTafsirForAyah(surah, ayah, lang, tafsirKey) ?: return null
        // Enrich with local surah metadata (names) if available
        val surahMeta = runCatching { metadataDataSource.getSurah(surah) }.getOrNull()
        return result.copy(
            surahNameArabic = surahMeta?.nameAr ?: "",
            surahNameEnglish = surahMeta?.nameEn ?: "",
        )
    }

    override suspend fun getAvailableTafsirBooks(): List<TafsirBook> {
        return try {
            val remoteBooks = tafsirRemoteDataSource?.getAvailableTafsirBooks() ?: emptyList()
            // Map local download state
            remoteBooks.map { book ->
                val isDownloaded = tafsirDownloadManager?.isDownloaded(book.tafsirKey) ?: false
                val state = if (isDownloaded) {
                    com.example.mushaf.domain.model.DownloadState.Downloaded
                } else {
                    com.example.mushaf.domain.model.DownloadState.NotDownloaded
                }
                book.copy(state = state)
            }
        } catch (e: Exception) {
            Log.e(MushafLog.TAG, "Failed to fetch remote tafsir books, using local files", e)
            val downloadedKeys = tafsirDownloadManager?.getDownloadedTafsirKeys() ?: emptyList()
            val keys = (downloadedKeys + "mukhtasar").distinct()
            keys.map { key ->
                TafsirBook(
                    tafsirKey = key,
                    displayName = key, // UI uses resources to translate this
                    language = "ar",
                    languageName = "العربية",
                    downloadUrl = "",
                    fileSizeBytes = 0,
                    state = com.example.mushaf.domain.model.DownloadState.Downloaded
                )
            }
        }
    }
    
    override fun observeAvailableTafsirBooks(): Flow<List<TafsirBook>> = flow {
        // The bundled "التفسير المختصر" is always available — it lives in SQLite assets
        // and never needs downloading. We prepend it unconditionally.
        val mukhtasarBook = TafsirBook(
            tafsirKey = "mukhtasar",
            displayName = "التفسير المختصر",
            language = "ar",
            languageName = "العربية",
            downloadUrl = "",
            fileSizeBytes = 0,
            state = com.example.mushaf.domain.model.DownloadState.Downloaded,
        )

        // Fetch downloadable books from the remote catalogue (best-effort).
        val remoteBooks: List<TafsirBook> = try {
            tafsirRemoteDataSource?.getAvailableTafsirBooks() ?: emptyList()
        } catch (e: Exception) {
            Log.e(MushafLog.TAG, "Failed to fetch remote tafsir catalogue", e)
            emptyList()
        }

        if (tafsirDownloadManager != null) {
            // Observe live download-state changes and reflect them in the list.
            tafsirDownloadManager.downloadStates.collect { states ->
                val updatedRemote = remoteBooks.map { book ->
                    val liveState = states[book.tafsirKey]
                        ?: if (tafsirDownloadManager.isDownloaded(book.tafsirKey))
                            com.example.mushaf.domain.model.DownloadState.Downloaded
                        else
                            com.example.mushaf.domain.model.DownloadState.NotDownloaded
                    book.copy(state = liveState)
                }
                emit(listOf(mukhtasarBook) + updatedRemote)
            }
        } else {
            emit(listOf(mukhtasarBook) + remoteBooks)
        }
    }

    override suspend fun downloadTafsirBook(tafsirKey: String, downloadUrl: String) {
        tafsirDownloadManager?.downloadTafsir(tafsirKey, downloadUrl)
    }

    override fun deleteTafsirBook(tafsirKey: String) {
        tafsirDownloadManager?.deleteTafsir(tafsirKey)
    }

    override suspend fun getTafsirFromLocalJson(tafsirKey: String, surah: Int, ayah: Int): TafsirResult? {
        return tafsirLocalJsonDataSource?.getTafsirForAyah(tafsirKey, surah, ayah)?.let { raw ->
            val surahMeta = metadataDataSource.getSurah(raw.surahNumber)
            raw.copy(
                surahNameArabic = surahMeta?.nameAr ?: "",
                surahNameEnglish = surahMeta?.nameEn ?: ""
            )
        }
    }

    override suspend fun searchTafsir(query: String, limit: Int, offset: Int): Result<List<TafsirResult>> =
        resultOf(mapError) {
            val rawResults = tafsirDataSource.searchTafsir(query, limit, offset)
            rawResults.map { raw ->
                val surahMeta = metadataDataSource.getSurah(raw.surahNumber)
                raw.copy(
                    surahNameArabic = surahMeta?.nameAr ?: "",
                    surahNameEnglish = surahMeta?.nameEn ?: ""
                )
            }
        }

    /**
     * Builds word-level entries by splitting [QuranTextDataSource]'s plain verse text on
     * whitespace and addressing each token with the same `sura:aya:word` scheme the rest of the
     * recitation pipeline uses ([RecitationCursor.wordId]) — no separate word-level corpus asset
     * needed. Word order within an ayah is inherently sequential, so this only requires knowing
     * *how many* real (non-end-marker) words the layout DB has for that ayah, to detect the
     * rasm/plain-text word-count mismatches noted on [LocalWordCorpusRepository.wordsFrom] and
     * skip them.
     */
    override suspend fun wordsFrom(cursor: RecitationCursor, count: Int): Result<List<LocalWordEntry>> =
        resultOf(mapError) {
            if (count <= 0) return@resultOf emptyList()
            val result = ArrayList<LocalWordEntry>(count)

            var sura = cursor.sura
            var aya = cursor.aya
            var startWordIndex = cursor.wordIndex
            var scanned = 0

            while (result.size < count && scanned < MAX_AYAHS_SCANNED) {
                scanned++
                val plainText = textDataSource.getVerseText(sura, aya)
                if (plainText == null) {
                    if (!advanceToNextSurah(sura)) break
                    sura += 1
                    aya = 1
                    startWordIndex = 0
                    continue
                }

                val plainWords = plainText.split(WHITESPACE)
                val realCount = dataSource.wordCountForAyah(sura, aya)
                if (plainWords.size != realCount) {
                    Log.d(
                        MushafLog.TAG,
                        "Local corpus: skipping $sura:$aya (plain=${plainWords.size}, layout=$realCount)",
                    )
                } else {
                    for (index in startWordIndex until plainWords.size) {
                        if (result.size >= count) break
                        result += LocalWordEntry(
                            wordId = "$sura:$aya:${index + 1}",
                            plainText = plainWords[index],
                        )
                    }
                }

                aya += 1
                startWordIndex = 0
            }

            result
        }

    /** No ayah-count table is queried; instead we just check surah 114 has already been passed,
     * since [QuranTextDataSource.getVerseText] returning null for `(sura, 1)` beyond it means
     * the corpus is exhausted. */
    private fun advanceToNextSurah(currentSura: Int): Boolean = currentSura < LAST_SURAH

    private companion object {
        const val LAST_SURAH = 114
        const val MAX_AYAHS_SCANNED = 30
        val WHITESPACE = Regex("\\s+")
    }
}
