package com.example.mushaf.domain

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.Surah
import com.example.mushaf.domain.model.TafsirResult
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.usecase.GetPageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPageUseCaseTest {

    private class RecordingRepository : MushafRepository {
        var requestedPage: Int = -1
        override fun getPage(pageNumber: Int): Flow<MushafPage> {
            requestedPage = pageNumber
            return flowOf(MushafPage(pageNumber, emptyList()))
        }
        override suspend fun getPageCount(): Int = 604
        override suspend fun searchSurah(query: String): List<Surah> = emptyList()
        override suspend fun searchJuz(query: String): List<Juz> = emptyList()
        override suspend fun searchHizb(query: String): List<Hizb> = emptyList()
        override suspend fun searchPage(query: String): List<Int> = emptyList()
        override suspend fun searchAyah(query: String, limit: Int, offset: Int): List<AyahSearchResult> = emptyList()
        override suspend fun searchAyahByMeaning(query: String, mode: String, hyde: Boolean, limit: Int): List<AyahSearchResult> = emptyList()
        override suspend fun getSurahStartingPage(surahNumber: Int): Int? = null
        override suspend fun getAyahPage(surahNumber: Int, ayahNumber: Int): Int? = null
        override suspend fun getJuzStartingPage(juzNumber: Int): Int? = null
        override suspend fun getTafsirForAyah(surah: Int, ayah: Int): TafsirResult? = null
        override suspend fun searchTafsir(query: String, limit: Int, offset: Int): List<TafsirResult> = emptyList()
    }

    @Test
    fun `clamps below range to first page`() = runTest {
        val repo = RecordingRepository()
        GetPageUseCase(repo)(0).first()
        assertEquals(1, repo.requestedPage)
    }

    @Test
    fun `clamps above range to last page`() = runTest {
        val repo = RecordingRepository()
        GetPageUseCase(repo)(9999).first()
        assertEquals(604, repo.requestedPage)
    }

    @Test
    fun `passes valid page through unchanged`() = runTest {
        val repo = RecordingRepository()
        GetPageUseCase(repo)(42).first()
        assertEquals(42, repo.requestedPage)
    }
}
