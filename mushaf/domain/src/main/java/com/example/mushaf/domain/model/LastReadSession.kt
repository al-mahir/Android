package com.example.mushaf.domain.model

data class LastReadSession(
    val surahNameAr: String,
    val surahNameEn: String,
    val ayah: Int,
    val page: Int
)
