package com.example.mushaf.data.repository

import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.db.MushafAssetDataSource
import com.example.mushaf.data.mapper.MushafMapper
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.repository.MushafRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MushafRepositoryImpl(
    private val dataSource: MushafAssetDataSource,
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
}
