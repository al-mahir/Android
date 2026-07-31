package com.example.mushaf.data.tafsir.remote

import com.example.mushaf.domain.model.TafsirBook
import com.example.mushaf.domain.model.TafsirResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
private data class ApiWrapper<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val timestamp: String? = null,
)

@Serializable
private data class TafsirDataDto(
    val surah: Int,
    val ayah: Int,
    val text: String,
)

@Serializable
private data class TafsirBookDto(
    val tafsirKey: String,
    val displayName: String,
    val language: String,
    val languageName: String,
    val downloadUrl: String,
    val fileSizeBytes: Long = 0L,
)

// ── Remote Data Source ────────────────────────────────────────────────────────

class TafsirRemoteDataSource(
    private val client: HttpClient,
    private val baseUrl: String = "https://almahir-production.up.railway.app",
) {

    /**
     * Fetch Tafsir for a specific Ayah from the backend.
     * Returns null if the server responds with success=false or empty data.
     * Throws on network error (let the caller decide on fallback strategy).
     */
    suspend fun getTafsirForAyah(
        surah: Int,
        ayah: Int,
        lang: String = "ar",
        tafsirKey: String = "ibn-kathir",
    ): TafsirResult? {
        val response = client.get("$baseUrl/api/tafsir") {
            parameter("surah", surah)
            parameter("ayah", ayah)
            parameter("lang", lang)
            parameter("tafsir", tafsirKey)
        }.body<ApiWrapper<TafsirDataDto>>()

        val data = response.data ?: return null
        if (data.text.isBlank()) return null

        return TafsirResult(
            surahNumber = data.surah,
            ayahNumber = data.ayah,
            tafsirText = data.text,
            tafsirKey = tafsirKey,
        )
    }

    /**
     * Retrieve the list of all available Tafsir books from the backend.
     * Returns empty list on failure.
     */
    suspend fun getAvailableTafsirBooks(): List<TafsirBook> {
        return runCatching {
            val response = client.get("$baseUrl/api/tafsir/available")
                .body<ApiWrapper<List<TafsirBookDto>>>()
            response.data?.map { dto ->
                TafsirBook(
                    tafsirKey = dto.tafsirKey,
                    displayName = dto.displayName,
                    language = dto.language,
                    languageName = dto.languageName,
                    downloadUrl = dto.downloadUrl,
                    fileSizeBytes = dto.fileSizeBytes,
                )
            } ?: emptyList()
        }.getOrElse { emptyList() }
    }
}
