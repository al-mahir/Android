package com.example.designsystem.components.bullet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A two-tone bullet marker — an outer translucent halo with a solid inner core, both
 * tinted with [color]. Use as the leading dot in bullet lists (meal items, nutritional
 * groups, etc.) so the marker reads as a small target instead of a flat dot.
 */
@Composable
fun AccentBullet(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.55f)
                .clip(CircleShape)
                .background(color),
        )
    }
}
