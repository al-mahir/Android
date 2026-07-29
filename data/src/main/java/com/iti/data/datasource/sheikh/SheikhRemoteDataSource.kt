package com.iti.data.datasource.sheikh

import com.iti.data.core.network.AlmahirApi
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.dto.sheikh.SheikhApiDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Real Ktor-backed implementation of [SheikhDataSource].
 *
 * Endpoints:
 *  - GET /api/sheikh           → list of all sheikhs
 *  - GET /api/sheikh/{id}      → single sheikh
 *  - GET /api/sheikh/search?name={name} → search results
 *
 * All responses use the standard ApiResponse<T> envelope:
 *  { "success": true, "data": T }
 */
class SheikhRemoteDataSource(
    private val httpClient: HttpClient,
) : SheikhDataSource {

    override suspend fun getSheikhs(): List<SheikhApiDto> {
        val response = httpClient.get(AlmahirApi.Sheikh.ALL)
        return response.body<ApiResponse<List<SheikhApiDto>>>().data.orEmpty()
    }

    override suspend fun getSheikhById(id: String): SheikhApiDto? {
        val response = httpClient.get("${AlmahirApi.Sheikh.ALL}/$id")
        return response.body<ApiResponse<SheikhApiDto>>().data
    }

    override suspend fun searchSheikhs(name: String): List<SheikhApiDto> {
        val response = httpClient.get(AlmahirApi.Sheikh.SEARCH) {
            parameter("name", name)
        }
        return response.body<ApiResponse<List<SheikhApiDto>>>().data.orEmpty()
    }
}
