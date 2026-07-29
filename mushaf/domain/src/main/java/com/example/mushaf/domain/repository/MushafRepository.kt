package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.Surah
import com.example.mushaf.domain.model.TafsirBook
import com.example.mushaf.domain.model.TafsirResult
import kotlinx.coroutines.flow.Flow


interface MushafRepository {

    fun getPage(pageNumber: Int): Flow<MushafPage>

    suspend fun getPageCount(): Int

    suspend fun searchSurah(query: String): List<Surah>
    suspend fun searchJuz(query: String): List<Juz>
    suspend fun searchHizb(query: String): List<Hizb>
    suspend fun searchPage(query: String): List<Int>
    suspend fun searchAyah(query: String, limit: Int = 50, offset: Int = 0): List<AyahSearchResult>
    suspend fun searchAyahByMeaning(query: String, mode: String = "hybrid", hyde: Boolean = true, limit: Int = 20): List<AyahSearchResult>

    suspend fun getSurahStartingPage(surahNumber: Int): Int?
    suspend fun getAyahPage(surahNumber: Int, ayahNumber: Int): Int?
    suspend fun getJuzStartingPage(juzNumber: Int): Int?

    /** Fetch Tafsir from the local offline SQLite database. */
    suspend fun getTafsirForAyah(surah: Int, ayah: Int): TafsirResult?

    /** Fetch Tafsir from the remote backend API (may throw on network failure). */
    suspend fun getTafsirFromApi(
        surah: Int,
        ayah: Int,
        lang: String = "ar",
        tafsirKey: String = "ibn-kathir",
    ): TafsirResult?

    /** List all Tafsir books available for selection from the backend. */
    suspend fun getAvailableTafsirBooks(): List<TafsirBook>
    fun observeAvailableTafsirBooks(): Flow<List<TafsirBook>>

    suspend fun downloadTafsirBook(tafsirKey: String, downloadUrl: String)
    fun deleteTafsirBook(tafsirKey: String)

    suspend fun getTafsirFromLocalJson(tafsirKey: String, surah: Int, ayah: Int): TafsirResult?

    suspend fun searchTafsir(query: String, limit: Int = 50, offset: Int = 0): List<TafsirResult>
}

