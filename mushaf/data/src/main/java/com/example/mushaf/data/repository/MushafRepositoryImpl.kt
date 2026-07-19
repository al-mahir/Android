package com.example.mushaf.data.repository

import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.db.MushafAssetDataSource
import com.example.mushaf.data.db.QuranMetadataDataSource
import com.example.mushaf.data.db.QuranTextDataSource
import com.example.mushaf.data.mapper.MushafMapper
import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.Surah
import com.example.mushaf.domain.repository.MushafRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MushafRepositoryImpl(
    private val dataSource: MushafAssetDataSource,
    private val metadataDataSource: QuranMetadataDataSource,
    private val textDataSource: QuranTextDataSource
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
}
