package com.iti.data.local.bookmark

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val type: String,
    val surahNumber: Int?,
    val ayahNumber: Int?,
    val pageNumber: Int?,
    val sheikhId: String?,
    val note: String?,
    val createdAtEpochMillis: Long
)
