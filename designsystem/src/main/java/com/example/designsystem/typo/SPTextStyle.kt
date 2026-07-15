package com.example.designsystem.typo

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

data class SPTextStyle(
    val display: TextStyle,
    val title: TextStyle,
    val body: SizedTextStyle,
    val hint: SizedTextStyle
)

internal val LocalSPTypography = staticCompositionLocalOf {
    spTypographyOf(arabicFontFamily)
}

internal val LocalSPFontFamily = staticCompositionLocalOf {
    arabicFontFamily
}
