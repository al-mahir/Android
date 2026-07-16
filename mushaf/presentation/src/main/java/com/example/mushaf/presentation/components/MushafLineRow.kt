package com.example.mushaf.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.MushafLine


@Composable
fun MushafLineRow(
    line: MushafLine,
    fontFamily: FontFamily,
    fontSize: TextUnit,
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
                        highlightColor = highlightColor,
                        highlightedWordId = highlightedWordId,
                    )
                }
            }
        }

        LineType.SURAH_NAME -> Text(
            text = SurahInfo.headerFor(line.surahNumber),
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            modifier = modifier.fillMaxWidth(),
        )

        LineType.BASMALLAH -> Text(
            text = SurahInfo.BASMALLAH,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            modifier = modifier.fillMaxWidth(),
        )
    }
}
