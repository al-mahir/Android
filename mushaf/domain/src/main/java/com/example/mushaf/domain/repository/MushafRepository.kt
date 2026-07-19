package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.Surah
import kotlinx.coroutines.flow.Flow


interface MushafRepository {

    fun getPage(pageNumber: Int): Flow<MushafPage>

    suspend fun getPageCount(): Int

    suspend fun searchSurah(query: String): List<Surah>
    suspend fun searchJuz(query: String): List<Juz>
    suspend fun searchHizb(query: String): List<Hizb>
    suspend fun searchPage(query: String): List<Int>
    suspend fun searchAyah(query: String): List<AyahSearchResult>
}
