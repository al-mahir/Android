package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.Surah
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

    suspend fun getTafsirForAyah(surah: Int, ayah: Int): Result<com.example.mushaf.domain.model.TafsirResult?>
    suspend fun searchTafsir(query: String, limit: Int = 50, offset: Int = 0): Result<List<com.example.mushaf.domain.model.TafsirResult>>
}
