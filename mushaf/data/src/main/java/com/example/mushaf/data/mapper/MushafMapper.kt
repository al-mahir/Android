package com.example.mushaf.data.mapper

import com.example.mushaf.data.db.MushafLineEntity
import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.MushafLine
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.MushafWord

/**
 * Maps the flat, line-based `pages` rows into the nested domain model, expanding each ayah
 * line's `first_word_id..last_word_id` range into individually-addressable words.
 */
object MushafMapper {

    fun toDomain(pageNumber: Int, rows: List<MushafLineEntity>): MushafPage {
        // The page's first word id anchors the per-page glyph codepoints.
        val pageFirstWordId = rows
            .filter { it.lineType == LINE_TYPE_AYAH }
            .mapNotNull { it.firstWordId?.takeIf { id -> id > 0 } }
            .minOrNull() ?: 0

        val lines = rows
            .sortedBy { it.lineNumber }
            .map { row -> row.toLine(pageNumber, pageFirstWordId) }

        return MushafPage(pageNumber = pageNumber, lines = lines)
    }

    private fun MushafLineEntity.toLine(pageNumber: Int, pageFirstWordId: Int): MushafLine {
        val type = lineType.toLineType()
        val words = if (type == LineType.AYAH) {
            expandWords(pageNumber, pageFirstWordId)
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

    private fun MushafLineEntity.expandWords(pageNumber: Int, pageFirstWordId: Int): List<MushafWord> {
        val first = firstWordId ?: return emptyList()
        val last = lastWordId ?: return emptyList()
        if (first <= 0 || last < first) return emptyList()
        return (first..last).mapIndexed { index, wordId ->
            MushafWord(
                id = "p$pageNumber:l$lineNumber:w$wordId",
                wordId = wordId,
                pageNumber = pageNumber,
                lineNumber = lineNumber,
                positionInLine = index,
                glyphCode = GlyphCodeResolver.resolve(pageFirstWordId, wordId),
            )
        }
    }

    private fun String.toLineType(): LineType = when (this) {
        LINE_TYPE_AYAH -> LineType.AYAH
        LINE_TYPE_BASMALLAH -> LineType.BASMALLAH
        LINE_TYPE_SURAH_NAME -> LineType.SURAH_NAME
        else -> LineType.AYAH
    }

    private const val LINE_TYPE_AYAH = "ayah"
    private const val LINE_TYPE_BASMALLAH = "basmallah"
    private const val LINE_TYPE_SURAH_NAME = "surah_name"
}
