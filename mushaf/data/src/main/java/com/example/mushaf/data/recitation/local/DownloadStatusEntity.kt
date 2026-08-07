package com.example.mushaf.data.recitation.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_status")
data class DownloadStatusEntity(
    @PrimaryKey
    val id: String, // format: "reciterId_surahId" or "reciterId_full"
    val reciterId: Int,
    val surahId: Int?, // null if full Quran download
    val progress: Int, // 0 to 100
    val state: String, // "PENDING", "DOWNLOADING", "COMPLETED", "ERROR"
    val totalSizeBytes: Long,
    val downloadedBytes: Long,
    val errorMessage: String?
) {
    val isCompleted: Boolean get() = state == "COMPLETED"
    val isDownloading: Boolean get() = state == "DOWNLOADING" || state == "PENDING"
}
