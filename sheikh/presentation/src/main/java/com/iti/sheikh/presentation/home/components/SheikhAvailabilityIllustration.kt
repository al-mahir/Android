package com.iti.sheikh.presentation.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.theme.Theme
import com.iti.domain.model.SheikhAvailability
import com.iti.sheikh.presentation.R

/**
 * Centered visual for the sheikh's current availability — a "live/broadcasting" pulse while
 * [SheikhAvailability.AVAILABLE], a calm icon otherwise. Purely decorative context for the
 * switch/status card above it, not an interactive control.
 */
@Composable
fun SheikhAvailabilityIllustration(
    availability: SheikhAvailability,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Theme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        when (availability) {
            SheikhAvailability.AVAILABLE -> PulsingAvailabilityDot()

            SheikhAvailability.IN_SESSION -> Image(
                painter = painterResource(DesignSystemR.drawable.ic_headphones),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Theme.colors.error),
                modifier = Modifier.size(96.dp),
            )

            SheikhAvailability.OFFLINE -> Image(
                painter = painterResource(DesignSystemR.drawable.ic_empty_data),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Theme.colors.hint),
                modifier = Modifier.size(96.dp),
            )
        }

        BasicText(
            text = stringResource(availability.illustrationTitleRes()),
            style = Theme.typography.title.copy(
                color = Theme.colors.primaryFont,
                textAlign = TextAlign.Center,
            ),
        )
        BasicText(
            text = stringResource(availability.illustrationDescriptionRes()),
            style = Theme.typography.body.medium.copy(
                color = Theme.colors.secondaryFont,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun PulsingAvailabilityDot(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sheikhAvailabilityPulse")
    val ringScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringScale",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringAlpha",
    )

    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                    alpha = ringAlpha
                }
                .clip(CircleShape)
                .background(Theme.colors.success),
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Theme.colors.success),
        )
    }
}

private fun SheikhAvailability.illustrationTitleRes(): Int = when (this) {
    SheikhAvailability.AVAILABLE -> R.string.sheikh_home_illustration_available_title
    SheikhAvailability.IN_SESSION -> R.string.sheikh_home_illustration_in_session_title
    SheikhAvailability.OFFLINE -> R.string.sheikh_home_illustration_offline_title
}

private fun SheikhAvailability.illustrationDescriptionRes(): Int = when (this) {
    SheikhAvailability.AVAILABLE -> R.string.sheikh_home_illustration_available_description
    SheikhAvailability.IN_SESSION -> R.string.sheikh_home_illustration_in_session_description
    SheikhAvailability.OFFLINE -> R.string.sheikh_home_illustration_offline_description
}
