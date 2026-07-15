package com.example.designsystem.components.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.designsystem.theme.Theme

private const val ShimmerDurationMs = 1200

/**
 * A sweeping gradient placeholder for content that is still loading (e.g. an image whose
 * bytes have not arrived yet). Apply to any sized surface — it paints an animated
 * left-to-right highlight band over a muted base.
 *
 * Reads its colours from [Theme] so it adapts to the active palette. Pair with
 * [ShimmerBox] when you just need a standalone placeholder rectangle.
 */
@Composable
fun Modifier.shimmer(): Modifier {
    val base = Theme.colors.surfaceVariant
    val highlight = Theme.colors.surface
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ShimmerDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-progress",
    )
    return this.drawWithCache {
        val width = size.width
        // Slide a band of (highlight) one full width across the surface each cycle.
        val start = (progress * 2f - 1f) * width
        val brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(start, 0f),
            end = Offset(start + width, size.height),
        )
        onDrawBehind { drawRect(brush = brush) }
    }
}

/** Convenience standalone shimmer rectangle. Size it via [modifier]. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.shimmer())
}

/** A flat, non-animated placeholder used when an image fails to load. */
@Composable
fun ImagePlaceholder(modifier: Modifier = Modifier, color: Color = Theme.colors.surfaceVariant) {
    Box(modifier = modifier.then(Modifier.drawWithCache { onDrawBehind { drawRect(color) } }))
}
