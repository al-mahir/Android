package com.example.mushaf.domain.model

data class TafsirBook(
    val tafsirKey: String,
    val displayName: String,
    val language: String,
    val languageName: String,
    val downloadUrl: String,
    val fileSizeBytes: Long,
)
