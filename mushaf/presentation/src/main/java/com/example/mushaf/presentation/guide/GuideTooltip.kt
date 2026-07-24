package com.example.mushaf.presentation.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

/**
 * Full-screen guide overlay that positions a tooltip card near the top,
 * centre, or bottom of the screen depending on [anchor], and draws a small
 * arrow pointing toward the target element.  No coordinate tracking is
 * required — the layout is driven entirely by the device's actual dimensions
 * via [BoxWithConstraints] and Compose's built-in inset helpers.
 */
@Composable
fun GuideTooltip(
    visible: Boolean,
    stepCounterText: String,
    instructionText: String,
    nextButtonText: String,
    anchor: TooltipAnchor,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // Full-screen scrim — tap anywhere to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss, indication = null,
                    interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource()),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

                // ── Tooltip column: arrow + card ───────────────────────────
                // For TOP targets  → card rendered from the top, arrow above it pointing UP
                // For BOTTOM targets → card rendered from the bottom, arrow below it pointing DOWN
                // For CENTER targets → card centred, no arrow

                val cardMaxWidth = maxWidth.coerceAtMost(360.dp)

                when (anchor) {

                    TooltipAnchor.TOP -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 56.dp, start = 16.dp, end = 16.dp),
                        ) {
                            // Arrow points UP toward the top bar
                            ArrowUp(color = Theme.colors.surface)
                            TooltipCard(
                                stepCounterText = stepCounterText,
                                instructionText = instructionText,
                                nextButtonText = nextButtonText,
                                maxWidth = cardMaxWidth,
                                onNext = onNext,
                            )
                        }
                    }

                    TooltipAnchor.BOTTOM -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                        ) {
                            TooltipCard(
                                stepCounterText = stepCounterText,
                                instructionText = instructionText,
                                nextButtonText = nextButtonText,
                                maxWidth = cardMaxWidth,
                                onNext = onNext,
                            )
                            // Arrow points DOWN toward the bottom bar
                            ArrowDown(color = Theme.colors.surface)
                        }
                    }

                    TooltipAnchor.CENTER -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                        ) {
                            TooltipCard(
                                stepCounterText = stepCounterText,
                                instructionText = instructionText,
                                nextButtonText = nextButtonText,
                                maxWidth = cardMaxWidth,
                                onNext = onNext,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Private helpers
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TooltipCard(
    stepCounterText: String,
    instructionText: String,
    nextButtonText: String,
    maxWidth: androidx.compose.ui.unit.Dp,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column {
            Text(
                text = stepCounterText,
                style = Theme.typography.body.small,
                color = Theme.colors.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = instructionText,
                style = Theme.typography.body.medium,
                color = Theme.colors.onSurface,
            )
            TextButton(
                onClick = onNext,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = nextButtonText,
                    style = Theme.typography.body.large,
                    color = Theme.colors.primary,
                )
            }
        }
    }
}

/** A small upward-pointing triangle arrow in the given [color]. */
@Composable
private fun ArrowUp(color: androidx.compose.ui.graphics.Color) {
    val arrowWidthDp  = 24.dp
    val arrowHeightDp = 12.dp
    Canvas(modifier = Modifier
        .widthIn(min = arrowWidthDp, max = arrowWidthDp)
        .height(arrowHeightDp)
        .graphicsLayer { }
    ) {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color = color)
    }
}

/** A small downward-pointing triangle arrow in the given [color]. */
@Composable
private fun ArrowDown(color: androidx.compose.ui.graphics.Color) {
    val arrowWidthDp  = 24.dp
    val arrowHeightDp = 12.dp
    Canvas(modifier = Modifier
        .widthIn(min = arrowWidthDp, max = arrowWidthDp)
        .height(arrowHeightDp)
        .graphicsLayer { }
    ) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path, color = color)
    }
}
