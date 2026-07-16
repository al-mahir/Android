package com.example.mushaf.data.db


data class MushafLineEntity(
    val pageNumber: Int,
    val lineNumber: Int,
    val lineType: String,
    val isCentered: Int,
    val firstWordId: Int?,
    val lastWordId: Int?,
    val surahNumber: Int?,
)
