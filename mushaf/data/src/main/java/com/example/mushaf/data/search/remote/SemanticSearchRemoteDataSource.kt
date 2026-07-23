package com.example.mushaf.data.search.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SemanticSearchRemoteDataSource(
    private val searchApi: SearchApi
) {
    suspend fun searchByMeaning(
        query: String,
        mode: String = "hybrid",
        hyde: Boolean = true,
        limit: Int = 20
    ): SearchResponseDto = withContext(Dispatchers.IO) {
        searchApi.searchAyahs(
            query = query,
            mode = mode,
            hyde = hyde,
            limit = limit
        )
    }
}
