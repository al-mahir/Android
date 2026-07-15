package com.example.designsystem.typo

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.designsystem.R

internal val arabicFontFamily = FontFamily(
    Font(
        resId = R.font.urwgeometricarabic_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.urwgeometricarabic_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.urwgeometricarabic_medium,
        weight = FontWeight.Medium
    )
)