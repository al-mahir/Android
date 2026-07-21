package com.example.mushaf.data.recitation

import kotlinx.coroutines.flow.Flow

interface RecitationDataSource {
    fun observeReciters(): Flow<List<ReciterDto>>
    fun observeTimingsForPage(reciterId: Int, pageNumber: Int): Flow<List<AyahTimingDto>>
}
