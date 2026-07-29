package com.iti.sheikh.presentation.availability

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.iti.sheikh.presentation.R

/**
 * Availability status card — replaces the old raw-Material3 toggle row. Shows a pulsing
 * status dot (pulsing only while available, matching a "live" indicator), the current
 * state's title/subtitle, and a design-system-tinted [Switch] to flip it.
 */
@Composable
fun AvailabilityToggleRow(
    isAvailable: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isAvailable) Theme.colors.primaryContainer else Theme.colors.surfaceVariant,
        animationSpec = tween(durationMillis = 400),
        label = "availabilityContainerColor",
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (isAvailable) Theme.colors.success else Theme.colors.hint,
        animationSpec = tween(durationMillis = 400),
        label = "availabilityIndicatorColor",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Theme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvailabilityPulseDot(isPulsing = isAvailable, color = indicatorColor)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Theme.spacing.medium, end = Theme.spacing.small),
            ) {
                BasicText(
                    text = stringResource(
                        if (isAvailable) R.string.meetingrequest_availability_available_title
                        else R.string.meetingrequest_availability_offline_title,
                    ),
                    style = Theme.typography.body.large.copy(color = Theme.colors.primaryFont),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = stringResource(
                        if (isAvailable) R.string.meetingrequest_availability_available_subtitle
                        else R.string.meetingrequest_availability_offline_subtitle,
                    ),
                    style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = isAvailable,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Theme.colors.onPrimary,
                    checkedTrackColor = Theme.colors.primary,
                    checkedBorderColor = Theme.colors.primary,
                    uncheckedThumbColor = Theme.colors.onDisable,
                    uncheckedTrackColor = Theme.colors.disable,
                    uncheckedBorderColor = Theme.colors.disable,
                ),
            )
        }
    }
}

@Composable
private fun AvailabilityPulseDot(
    isPulsing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "availabilityPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = modifier.size(Theme.size.iconMedium),
        contentAlignment = Alignment.Center,
    ) {
        if (isPulsing) {
            Box(
                modifier = Modifier
                    .size(Theme.size.statusDot)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Box(
            modifier = Modifier
                .size(Theme.size.statusDot)
                .clip(CircleShape)
                .background(color),
        )
    }
}
