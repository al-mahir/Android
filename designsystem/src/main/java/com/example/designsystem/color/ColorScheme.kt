package com.example.designsystem.color

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ColorScheme(
    val primary: Color,
    val primaryVariant: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val backGround: Color,
    val primaryFont: Color,
    val secondaryFont: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val field: Color,
    val border: Color,
    val outline: Color,
    val hint: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val onSuccess: Color,
    val amber: Color,
    val onAmber: Color,
    val disable: Color,
    val onDisable: Color,
)
