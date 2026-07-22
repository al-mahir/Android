package com.example.mushaf.data.recite.remote

import com.example.mushaf.data.recite.remote.dto.HealthDto
import com.example.mushaf.data.recite.remote.dto.MoshafSchemaDto
import com.example.mushaf.data.recite.remote.dto.TajweedRulesResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get


class AiServiceApi(
    private val client: HttpClient,
    private val config: AiServiceConfig,
) {

    suspend fun health(): HealthDto =
        client.get("${config.httpBaseUrl}/health").body()

    suspend fun tajweedRules(): TajweedRulesResponseDto =
        client.get("${config.httpBaseUrl}/tajweed-rules").body()

    suspend fun moshafSchema(): MoshafSchemaDto =
        client.get("${config.httpBaseUrl}/moshaf-schema").body()
}
