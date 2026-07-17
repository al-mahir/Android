package com.example.mushaf.domain.model



data class MushafWord(
    val id: String,
    val glyphs: String,
    val pageNumber: Int,
    val lineNumber: Int,
    val positionInLine: Int,
    val isEndOfAyah: Boolean,
)
