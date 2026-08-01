package com.iti.domain.model

data class Bookmark(
    val id: String,
    val type: BookmarkType,
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null,
    val pageNumber: Int? = null,
    val sheikhId: String? = null,
    val note: String? = null,
    val createdAtEpochMillis: Long
) {
    companion object {

        fun buildId(
            type: BookmarkType,
            surahNumber: Int? = null,
            ayahNumber: Int? = null,
            pageNumber: Int? = null,
            sheikhId: String? = null,
        ): String = listOf(type.name, surahNumber, ayahNumber, pageNumber, sheikhId).joinToString(":")
    }
}
