package com.example.mushaf.presentation.components

import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.ReadingMode



object MushafLayoutCache {

    private const val CACHE_SIZE = 24

    private data class Key(
        val page: Int,
        val tajweed: Boolean,
        val widthPx: Int,
        val heightLimitMilliSp: Int,
    )

    private val cache = object : LinkedHashMap<Key, Map<Int, Float>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Key, Map<Int, Float>>): Boolean =
            size > CACHE_SIZE
    }

    @Synchronized
    private fun cached(key: Key): Map<Int, Float>? = cache[key]

    @Synchronized
    private fun store(key: Key, value: Map<Int, Float>) {
        cache[key] = value
    }

    


 
    fun lineSizes(
        page: MushafPage,
        mode: ReadingMode,
        widthPx: Int,
        heightLimitSp: Float,
        measureWidth: (String) -> Int,
    ): Map<Int, Float> {
        val key = Key(
            page = page.pageNumber,
            tajweed = mode == ReadingMode.TAJWEED,
            widthPx = widthPx,
            heightLimitMilliSp = (heightLimitSp * 1000f).toInt(),
        )
        cached(key)?.let { return it }
        val computed = compute(page, widthPx, heightLimitSp, measureWidth)
        store(key, computed)
        return computed
    }

    private fun compute(
        page: MushafPage,
        widthPx: Int,
        heightLimitSp: Float,
        measureWidth: (String) -> Int,
    ): Map<Int, Float> {
        val ayahWidths = page.lines
            .filter { it.type == LineType.AYAH && it.words.isNotEmpty() }
            .associate { line ->
                line.lineNumber to measureWidth(line.words.joinToString("") { it.glyphs })
            }
        val baseSize = MushafLayoutMath.uniformAyahSizeSp(
            lineWidthsPx = ayahWidths.values.toList(),
            maxWidthPx = widthPx,
            heightLimitSp = heightLimitSp,
        )
        return page.lines.associate { line ->
            val justified = line.type == LineType.AYAH && !line.isCentered && line.words.size > 1
            val width = ayahWidths[line.lineNumber]
            val size = if (justified && width != null) {
                MushafLayoutMath.fillLineSizeSp(width, widthPx, heightLimitSp)
            } else {
                baseSize
            }
            line.lineNumber to size
        }
    }
}
