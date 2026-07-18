package com.example.mushaf.data.db


data class MushafLineEntity(
    val pageNumber: Int,
    val lineNumber: Int,
    val lineType: String,
    val isCentered: Int,
    val surahNumber: Int?,
)

data class MushafWordEntity(
    val pageNumber: Int,
    val lineNumber: Int,
    val position: Int,
    val wordKey: String,
    val charType: String,
    val glyphText: String,
)
