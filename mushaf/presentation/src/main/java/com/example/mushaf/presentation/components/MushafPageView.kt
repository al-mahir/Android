package com.example.mushaf.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.model.MushafLine
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.ReadingMode
import com.example.mushaf.presentation.font.rememberPageFontFamily

private const val MIN_FONT_SP = 14f
private const val MAX_FONT_SP = 44f

private const val MEASURE_REF_SP = 40f


private const val FILL_SAFETY = 0.985f


@Composable
fun MushafPageView(
    page: MushafPage,
    mode: ReadingMode,
    highlightedWordId: () -> String?,
    modifier: Modifier = Modifier,
) {
    val fontFamily = rememberPageFontFamily(page.pageNumber, mode)
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        val availableWidthPx = constraints.maxWidth
        val availableHeightPx = constraints.maxHeight

        val pageFontSize: TextUnit =
            remember(page.pageNumber, mode, availableWidthPx, availableHeightPx) {
                uniformPageFontSize(
                    page = page,
                    maxWidthPx = availableWidthPx,
                    maxHeightPx = availableHeightPx,
                    measure = { text ->
                        measurer.measure(
                            text = AnnotatedString(text),
                            style = TextStyle(fontFamily = fontFamily, fontSize = MEASURE_REF_SP.sp),
                            softWrap = false,
                            maxLines = 1,
                        ).size
                    },
                )
            }

        Column(modifier = Modifier.fillMaxSize()) {
            for (slot in 1..MushafConstants.LINES_PER_PAGE) {
                val line: MushafLine? = page.lines.firstOrNull { it.lineNumber == slot }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (line != null) {
                        MushafLineRow(
                            line = line,
                            fontFamily = fontFamily,
                            fontSize = pageFontSize,
                            highlightColor = highlightColor,
                            highlightedWordId = highlightedWordId,
                        )
                    }
                }
            }
        }
    }
}


private fun uniformPageFontSize(
    page: MushafPage,
    maxWidthPx: Int,
    maxHeightPx: Int,
    measure: (String) -> androidx.compose.ui.unit.IntSize,
): TextUnit {
    val sizes = page.lines
        .filter { it.type == LineType.AYAH && it.words.isNotEmpty() }
        .map { line ->
            measure(line.words.joinToString("") { String(Character.toChars(it.glyphCode)) })
        }
        .filter { it.width > 0 && it.height > 0 }
    if (sizes.isEmpty()) return MAX_FONT_SP.sp

    val widestPx = sizes.maxOf { it.width }
    val tallestPx = sizes.maxOf { it.height }
    val slotHeightPx = maxHeightPx.toFloat() / MushafConstants.LINES_PER_PAGE

    val byWidth = MEASURE_REF_SP * (maxWidthPx.toFloat() / widestPx) * FILL_SAFETY
    val byHeight = MEASURE_REF_SP * (slotHeightPx / tallestPx)

    return minOf(byWidth, byHeight).coerceIn(MIN_FONT_SP, MAX_FONT_SP).sp
}
