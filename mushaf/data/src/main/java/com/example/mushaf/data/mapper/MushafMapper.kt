package com.example.mushaf.data.mapper

import com.example.mushaf.data.db.MushafLineEntity
import com.example.mushaf.data.db.MushafWordEntity
import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.MushafLine
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.MushafWord


object MushafMapper {

    fun toDomain(
        pageNumber: Int,
        lineRows: List<MushafLineEntity>,
        wordRows: List<MushafWordEntity>,
    ): MushafPage {
        val wordsByLine = wordRows.groupBy { it.lineNumber }
        val lines = lineRows
            .sortedBy { it.lineNumber }
            .map { row -> row.toLine(pageNumber, wordsByLine[row.lineNumber].orEmpty()) }
        return MushafPage(pageNumber = pageNumber, lines = lines)
    }

    private fun MushafLineEntity.toLine(pageNumber: Int, wordRows: List<MushafWordEntity>): MushafLine {
        val type = lineType.toLineType()
        val words = if (type == LineType.AYAH) {
            wordRows.sortedBy { it.position }.map { it.toWord(pageNumber) }
        } else {
            emptyList()
        }
        return MushafLine(
            lineNumber = lineNumber,
            type = type,
            isCentered = isCentered == 1,
            surahNumber = surahNumber?.takeIf { it > 0 },
            words = words,
        )
    }

    private fun MushafWordEntity.toWord(pageNumber: Int): MushafWord = MushafWord(
        id = if (wordKey.isNotBlank()) wordKey.trim() else "p$pageNumber:l$lineNumber:p$position",
        glyphs = glyphText,
        pageNumber = pageNumber,
        lineNumber = lineNumber,
        positionInLine = position,
        isEndOfAyah = charType == CHAR_TYPE_END,
    )

    private fun String.toLineType(): LineType = when (this) {
        LINE_TYPE_AYAH -> LineType.AYAH
        LINE_TYPE_BASMALLAH -> LineType.BASMALLAH
        LINE_TYPE_SURAH_NAME -> LineType.SURAH_NAME
        else -> LineType.AYAH
    }

    private const val LINE_TYPE_AYAH = "ayah"
    private const val LINE_TYPE_BASMALLAH = "basmallah"
    private const val LINE_TYPE_SURAH_NAME = "surah_name"
    private const val CHAR_TYPE_END = "end"
}
