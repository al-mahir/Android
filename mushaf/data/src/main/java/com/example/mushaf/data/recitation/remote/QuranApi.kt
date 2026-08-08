package com.example.mushaf.data.recitation.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class QuranApi(
    private val client: HttpClient
) {
    suspend fun getVersesByPage(
        pageNumber: Int,
        reciterId: Int = 7 
    ): QuranApiVersesResponse {
        return client.get("https://api.quran.com/api/v4/verses/by_page/$pageNumber") {
            parameter("audio", reciterId)
            parameter("words", "true")
        }.body()
    }

    suspend fun getVersesByChapter(
        chapterNumber: Int,
        reciterId: Int = 7
    ): QuranApiVersesResponse {
        return client.get("https://api.quran.com/api/v4/verses/by_chapter/$chapterNumber") {
            parameter("audio", reciterId)
            parameter("words", "true")
            parameter("per_page", "300") // get all verses in chapter
        }.body()
    }
}
