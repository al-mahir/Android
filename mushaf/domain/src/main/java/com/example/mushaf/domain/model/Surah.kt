package com.example.mushaf.domain.model

data class Surah(
    val number: Int,
    val nameEn: String,
    val nameAr: String,
    val revelationType: String,
    val verseCount: Int
)
