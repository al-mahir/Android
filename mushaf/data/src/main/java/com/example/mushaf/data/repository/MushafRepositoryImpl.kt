package com.example.mushaf.data.repository

import android.util.Log
import com.example.mushaf.data.MushafLog
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
import com.example.mushaf.domain.repository.MushafRepository
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
) : MushafRepository {

    override fun getPage(pageNumber: Int): Flow<MushafPage> = flow {
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
    }

    override suspend fun getPageCount(): Int = dataSource.getPageCount()

    override suspend fun searchSurah(query: String): List<Surah> {
        return metadataDataSource.searchSurah(query)
    }

    override suspend fun searchJuz(query: String): List<Juz> {
        return metadataDataSource.searchJuz(query)
    }

    override suspend fun searchHizb(query: String): List<Hizb> {
        return metadataDataSource.searchHizb(query)
    }

    override suspend fun searchPage(query: String): List<Int> {
        return dataSource.searchPage(query)
    }

    override suspend fun searchAyah(query: String, limit: Int, offset: Int): List<AyahSearchResult> {
        val rawResults = textDataSource.searchAyahs(query, limit, offset)
        return rawResults.map { raw ->
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
    ): List<AyahSearchResult> {
        val dataSource = semanticSearchDataSource ?: return emptyList()
        val response = dataSource.searchByMeaning(query, mode, hyde, limit)
        return response.hits.map { hit ->
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

    override suspend fun getSurahStartingPage(surahNumber: Int): Int? {
        return dataSource.getSurahStartingPage(surahNumber)
    }

    override suspend fun getAyahPage(surahNumber: Int, ayahNumber: Int): Int? {
        return dataSource.getAyahPage(surahNumber, ayahNumber)
    }

    override suspend fun getJuzStartingPage(juzNumber: Int): Int? {
        val juzPages = intArrayOf(
            1, 22, 42, 62, 82, 102, 122, 142, 162, 182, 
            202, 222, 242, 262, 282, 302, 322, 342, 362, 382, 
            402, 422, 442, 462, 482, 502, 522, 542, 562, 582
        )
        if (juzNumber in 1..30) {
            return juzPages[juzNumber - 1]
        }
        return null
    }

    override suspend fun getTafsirForAyah(surah: Int, ayah: Int): TafsirResult? {
        return tafsirDataSource.getTafsirForAyah(surah, ayah)?.let { raw ->
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
        val remoteBooks = getAvailableTafsirBooks()
        if (tafsirDownloadManager != null) {
            tafsirDownloadManager.downloadStates.collect { states ->
                val updated = remoteBooks.map { book ->
                    val currentState = states[book.tafsirKey] 
                        ?: if (tafsirDownloadManager.isDownloaded(book.tafsirKey)) com.example.mushaf.domain.model.DownloadState.Downloaded else com.example.mushaf.domain.model.DownloadState.NotDownloaded
                    book.copy(state = currentState)
                }
                emit(updated)
            }
        } else {
            emit(remoteBooks)
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

    override suspend fun searchTafsir(query: String, limit: Int, offset: Int): List<TafsirResult> {
        val rawResults = tafsirDataSource.searchTafsir(query, limit, offset)
        return rawResults.map { raw ->
            val surahMeta = metadataDataSource.getSurah(raw.surahNumber)
            raw.copy(
                surahNameArabic = surahMeta?.nameAr ?: "",
                surahNameEnglish = surahMeta?.nameEn ?: ""
            )
        }
    }
}
