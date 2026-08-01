package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.Surah
import com.example.mushaf.domain.model.TafsirBook
import com.example.mushaf.domain.model.TafsirResult
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow


interface MushafRepository {

    fun getPage(pageNumber: Int): Flow<Result<MushafPage>>

    suspend fun getPageCount(): Result<Int>

    suspend fun searchSurah(query: String): Result<List<Surah>>
    suspend fun searchJuz(query: String): Result<List<Juz>>
    suspend fun searchHizb(query: String): Result<List<Hizb>>
    suspend fun searchPage(query: String): Result<List<Int>>
    suspend fun searchAyah(query: String, limit: Int = 50, offset: Int = 0): Result<List<AyahSearchResult>>
    suspend fun searchAyahByMeaning(query: String, mode: String = "hybrid", hyde: Boolean = true, limit: Int = 20): Result<List<AyahSearchResult>>

    suspend fun getSurahStartingPage(surahNumber: Int): Result<Int?>
    suspend fun getAyahPage(surahNumber: Int, ayahNumber: Int): Result<Int?>
    suspend fun getJuzStartingPage(juzNumber: Int): Result<Int?>

    suspend fun getAyahText(surahNumber: Int, ayahNumber: Int): Result<String?>

    /** Fetch Tafsir from the local offline SQLite database. */
    suspend fun getTafsirForAyah(surah: Int, ayah: Int): Result<com.example.mushaf.domain.model.TafsirResult?>

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

    suspend fun searchTafsir(query: String, limit: Int = 50, offset: Int = 0): Result<List<com.example.mushaf.domain.model.TafsirResult>>
}

