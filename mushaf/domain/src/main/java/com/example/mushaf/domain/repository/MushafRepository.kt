package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.MushafPage
import kotlinx.coroutines.flow.Flow


interface MushafRepository {

    fun getPage(pageNumber: Int): Flow<MushafPage>

    suspend fun getPageCount(): Int
}
