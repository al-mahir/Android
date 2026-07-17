package com.example.mushaf.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.MushafLine
import com.example.mushaf.presentation.font.PageFontProvider


@Composable
fun MushafLineRow(
    line: MushafLine,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    surahNameFontFamily: FontFamily?,
    measurer: TextMeasurer,
    maxWidthPx: Int,
    slotHeightPx: Float,
    contentColor: Color,
    highlightColor: Color,
    highlightedWordId: () -> String?,
    modifier: Modifier = Modifier,
) {
    when (line.type) {
        LineType.AYAH -> {
            val centered = line.isCentered || line.words.size <= 1
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement =
                    if (centered) Arrangement.Center else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                line.words.forEach { word ->
                    MushafWordGlyph(
                        word = word,
                        fontFamily = fontFamily,
                        fontSize = fontSize,
                        contentColor = contentColor,
                        highlightColor = highlightColor,
                        highlightedWordId = highlightedWordId,
                    )
                }
            }
        }

        LineType.SURAH_NAME -> {
            val surahNumber = line.surahNumber ?: 1
            val glyph = remember(surahNumber) { PageFontProvider.surahNameGlyph(surahNumber) }
            if (surahNameFontFamily != null && glyph != null) {
                val size = remember(surahNumber, maxWidthPx, slotHeightPx) {
                    fitLineSize(glyph, surahNameFontFamily, measurer, maxWidthPx, slotHeightPx)
                }
                Text(
                    text = glyph,
                    style = TextStyle(
                        fontFamily = surahNameFontFamily,
                        fontSize = size,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                    ),
                    softWrap = false,
                    maxLines = 1,
                    modifier = modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = SurahInfo.headerFor(line.surahNumber),
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontSize = 22.sp,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    modifier = modifier.fillMaxWidth(),
                )
            }
        }

        LineType.BASMALLAH -> {
            // U+FDFD is the single-glyph ornamental basmala ligature, present in the system
            // Naskh Arabic font — reliable and calligraphic without shipping a shaping font.
            val size = remember(maxWidthPx, slotHeightPx) {
                fitLineSize(SurahInfo.BASMALLAH_LIGATURE, FontFamily.Default, measurer, maxWidthPx, slotHeightPx)
            }
            Text(
                text = SurahInfo.BASMALLAH_LIGATURE,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontSize = size,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                ),
                softWrap = false,
                maxLines = 1,
                modifier = modifier.fillMaxWidth(),
            )
        }
    }
}

/** Largest size that fits [text] (in [font]) within the line's width and vertical slot. */
private fun fitLineSize(
    text: String,
    font: FontFamily,
    measurer: TextMeasurer,
    maxWidthPx: Int,
    slotHeightPx: Float,
): TextUnit {
    val measured = measurer.measure(
        text = AnnotatedString(text),
        style = TextStyle(fontFamily = font, fontSize = MushafLayoutMath.REF_SP.sp),
        softWrap = false,
        maxLines = 1,
    ).size
    return MushafLayoutMath.fitSizeSp(
        contentWidthPx = measured.width,
        contentHeightPx = measured.height,
        maxWidthPx = maxWidthPx,
        maxHeightPx = slotHeightPx,
    ).sp
}
