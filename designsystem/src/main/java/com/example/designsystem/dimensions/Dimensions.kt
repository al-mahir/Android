package com.example.designsystem.dimensions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class SPSpacing(
    val default: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp
)

data class SPShapes(
    val small: Shape = RoundedCornerShape(4.dp),
    val medium: Shape = RoundedCornerShape(8.dp),
    val large: Shape = RoundedCornerShape(16.dp),
    val extraLarge: Shape = RoundedCornerShape(20.dp),
    val circle: Shape = RoundedCornerShape(50)
)
data class SPSize(
    val iconSmall: Dp = 12.dp,
    val iconSemiMedium: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val componentsNormalHeight: Dp = 48.dp,
    val componentsLargeHeight: Dp = 56.dp,
    val overlayContainer: Dp = 160.dp,
    val overlayIndicator: Dp = 80.dp,
    val statusDot: Dp = 8.dp,
    val avatarSmall: Dp = 40.dp,
    val avatarMedium: Dp = 56.dp,
    val iconContainer: Dp = 52.dp,
)

val LocalSPSpacing = staticCompositionLocalOf { SPSpacing() }
val LocalSPShapes = staticCompositionLocalOf { SPShapes() }
val LocalSPSize = staticCompositionLocalOf { SPSize() }
