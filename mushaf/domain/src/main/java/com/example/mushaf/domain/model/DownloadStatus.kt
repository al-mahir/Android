package com.example.mushaf.domain.model

data class DownloadStatus(
    val id: String,
    val reciterId: Int,
    val surahId: Int?,
    val progress: Int,
    val state: String,
    val errorMessage: String?
)
