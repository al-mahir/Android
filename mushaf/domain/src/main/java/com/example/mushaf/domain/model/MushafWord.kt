package com.example.mushaf.domain.model


data class MushafWord(
    val id: String,
    val wordId: Int,
    val pageNumber: Int,
    val lineNumber: Int,
    val positionInLine: Int,
    val glyphCode: Int,
)
