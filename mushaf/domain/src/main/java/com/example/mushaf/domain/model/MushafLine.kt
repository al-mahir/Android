package com.example.mushaf.domain.model


data class MushafLine(
    val lineNumber: Int,
    val type: LineType,
    val isCentered: Boolean,
    val surahNumber: Int?,
    val words: List<MushafWord>,
)
