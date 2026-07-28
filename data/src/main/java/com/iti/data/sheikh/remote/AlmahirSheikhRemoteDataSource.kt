package com.iti.data.sheikh.remote

import com.iti.data.core.network.AlmahirApi
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.dto.sheikh.SheikhApiDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface AlmahirSheikhRemoteDataSource {

    /** GET /api/sheikh/{id} for the signed-in sheikh's own id — their current server-side status. */
    suspend fun getMyProfile(id: String): SheikhApiDto?
}

class AlmahirSheikhRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : AlmahirSheikhRemoteDataSource {

    override suspend fun getMyProfile(id: String): SheikhApiDto? {
        val response = httpClient.get("${AlmahirApi.Sheikh.ALL}/$id")
        return response.body<ApiResponse<SheikhApiDto>>().data
    }
}
