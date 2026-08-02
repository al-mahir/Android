package com.example.mushaf.domain

import com.example.mushaf.domain.model.AyahSearchResult
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.Surah
import com.example.mushaf.domain.model.TafsirResult
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPageUseCaseTest {

    private class RecordingRepository : MushafRepository {
        var requestedPage: Int = -1
        override fun getPage(pageNumber: Int): Flow<Result<MushafPage>> {
            requestedPage = pageNumber
            return flowOf(Result.Success(MushafPage(pageNumber, emptyList())))
        }
        override suspend fun getPageCount(): Result<Int> = Result.Success(604)
        override suspend fun searchSurah(query: String): Result<List<Surah>> = Result.Success(emptyList())
        override suspend fun searchJuz(query: String): Result<List<Juz>> = Result.Success(emptyList())
        override suspend fun searchHizb(query: String): Result<List<Hizb>> = Result.Success(emptyList())
        override suspend fun searchPage(query: String): Result<List<Int>> = Result.Success(emptyList())
        override suspend fun searchAyah(query: String, limit: Int, offset: Int): Result<List<AyahSearchResult>> = Result.Success(emptyList())
        override suspend fun searchAyahByMeaning(query: String, mode: String, hyde: Boolean, limit: Int): Result<List<AyahSearchResult>> = Result.Success(emptyList())
        override suspend fun getSurahStartingPage(surahNumber: Int): Result<Int?> = Result.Success(null)
        override suspend fun getAyahPage(surahNumber: Int, ayahNumber: Int): Result<Int?> = Result.Success(null)
        override suspend fun getJuzStartingPage(juzNumber: Int): Result<Int?> = Result.Success(null)
        override suspend fun getAyahText(surahNumber: Int, ayahNumber: Int): Result<String?> = Result.Success(null)
        override suspend fun getTafsirForAyah(surah: Int, ayah: Int): Result<TafsirResult?> = Result.Success(null)
        override suspend fun searchTafsir(query: String, limit: Int, offset: Int): Result<List<TafsirResult>> = Result.Success(emptyList())
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
