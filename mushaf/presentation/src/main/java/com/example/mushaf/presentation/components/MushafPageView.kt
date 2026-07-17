package com.example.mushaf.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.ReadingMode
import com.example.mushaf.presentation.font.rememberPageFontFamily
import com.example.mushaf.presentation.font.rememberSurahNameFontFamily


@Composable
fun MushafPageView(
    page: MushafPage,
    mode: ReadingMode,
    highlightedWordId: () -> String?,
    modifier: Modifier = Modifier,
) {
    val fontFamily = rememberPageFontFamily(page.pageNumber, mode)
    val surahNameFontFamily = rememberSurahNameFontFamily()
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        val availableWidthPx = constraints.maxWidth
        val availableHeightPx = constraints.maxHeight
        val slotHeightPx = availableHeightPx.toFloat() / MushafConstants.LINES_PER_PAGE

        val slotHeightSp = slotHeightPx / density.density / density.fontScale
        val heightLimitSp = slotHeightSp / MushafLayoutMath.LINE_HEIGHT_EM
        val lineSizes: Map<Int, Float> =
            remember(page.pageNumber, mode, availableWidthPx, heightLimitSp) {
                val measureWordWidth: (String) -> Int = { glyphs ->
                    measurer.measure(
                        text = AnnotatedString(glyphs),
                        style = TextStyle(fontFamily = fontFamily, fontSize = MushafLayoutMath.REF_SP.sp),
                        softWrap = false,
                        maxLines = 1,
                    ).size.width
                }
                val ayahWidths = page.lines
                    .filter { it.type == LineType.AYAH && it.words.isNotEmpty() }
                    .associate { line -> line.lineNumber to line.words.sumOf { measureWordWidth(it.glyphs) } }
                // Centered/short lines and non-ayah lines use the widest justified line's size so
                // they read at the same scale as the surrounding text instead of being stretched.
                val baseSize = MushafLayoutMath.uniformAyahSizeSp(
                    lineWidthsPx = ayahWidths.values.toList(),
                    maxWidthPx = availableWidthPx,
                    heightLimitSp = heightLimitSp,
                )
                page.lines.associate { line ->
                    val justified = line.type == LineType.AYAH && !line.isCentered && line.words.size > 1
                    val width = ayahWidths[line.lineNumber]
                    val size = if (justified && width != null) {
                        MushafLayoutMath.fillLineSizeSp(width, availableWidthPx, heightLimitSp)
                    } else {
                        baseSize
                    }
                    line.lineNumber to size
                }
            }

        val slotHeightDp = with(density) { slotHeightPx.toDp() }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            page.lines.sortedBy { it.lineNumber }.forEach { line ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotHeightDp),
                    contentAlignment = Alignment.Center,
                ) {
                    MushafLineRow(
                        line = line,
                        fontFamily = fontFamily,
                        fontSize = (lineSizes[line.lineNumber] ?: MushafLayoutMath.MIN_SP).sp,
                        surahNameFontFamily = surahNameFontFamily,
                        measurer = measurer,
                        maxWidthPx = availableWidthPx,
                        slotHeightPx = slotHeightPx,
                        highlightColor = highlightColor,
                        highlightedWordId = highlightedWordId,
                    )
                }
            }
        }
    }
}
