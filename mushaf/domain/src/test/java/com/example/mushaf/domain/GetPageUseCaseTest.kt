package com.example.mushaf.domain

import com.example.mushaf.domain.model.MushafPage
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
