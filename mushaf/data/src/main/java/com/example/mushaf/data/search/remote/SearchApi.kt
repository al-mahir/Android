package com.example.mushaf.data.search.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchHitDto(
    val sura: Int,
    val aya: Int,
    @SerialName("text_uthmani") val textUthmani: String = "",
    val translation: String? = null,
    val score: Double? = null
)

@Serializable
data class SearchResponseDto(
    val hits: List<SearchHitDto> = emptyList(),
    @SerialName("matched_lang") val matchedLang: String? = null,
    val mode: String? = null,
    @SerialName("hyde_used") val hydeUsed: Boolean = false
)

class SearchApi(
    private val client: HttpClient,
    private val baseUrl: String = "http://10.0.2.2:8100"
) {
    suspend fun searchAyahs(
        query: String,
        mode: String = "hybrid",
        hyde: Boolean = true,
        lang: String? = null,
        limit: Int = 20
    ): SearchResponseDto {
        return client.get("$baseUrl/search") {
            parameter("q", query)
            parameter("mode", mode)
            parameter("hyde", hyde)
            if (!lang.isNull_or_blank()) {
                parameter("lang", lang)
            }
            parameter("limit", limit)
        }.body()
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
