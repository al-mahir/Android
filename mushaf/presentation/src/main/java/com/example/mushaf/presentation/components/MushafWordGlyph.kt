package com.example.mushaf.presentation.components

import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import com.example.mushaf.domain.model.MushafWord


@Composable
fun MushafWordGlyph(
    word: MushafWord,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    contentColor: Color,
    highlightColor: Color,
    highlightedWordId: () -> String?,
    modifier: Modifier = Modifier,
) {
    val isHighlighted by remember(word.id) {
        derivedStateOf { highlightedWordId() == word.id }
    }

    Text(
        text = word.glyphs,
        style = TextStyle(
            fontFamily = fontFamily,
            fontSize = fontSize,
            color = contentColor,
        ),
        softWrap = false,
        maxLines = 1,
        modifier = modifier
            .background(if (isHighlighted) highlightColor else Color.Transparent),
    )
}
