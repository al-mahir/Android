package com.example.designsystem.components.mushaf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.os.ConfigurationCompat
import com.example.designsystem.theme.Theme
import java.text.NumberFormat

/**
 * Share of the reciter's *scored* words that came back without a confident mistake.
 *
 * Two things it deliberately is not:
 *
 * - **Not a grade.** The service's own documentation calls the thresholds separating a hint from
 *   a mistake "uncalibrated placeholders", so this figure can move when the model is retuned
 *   without the reciter reciting any differently. Present it as session feedback, never as an
 *   assessment, a streak, or something to compete on.
 * - **Not over every word.** Words the chunker cut and never scored are excluded from both
 *   halves of the fraction, so an unverified word cannot quietly count as either a success or
 *   a failure.
 *
 * @param scoredWordCount denominator, shown so the number is readable as "so far" rather than final.
 */
@Composable
fun AccuracyPill(
    accuracy: Float,
    scoredWordCount: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]
    val percent = remember(accuracy, locale) {
        NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 1 }.format(accuracy)
    }
    val words = remember(scoredWordCount, locale) {
        NumberFormat.getIntegerInstance(locale).format(scoredWordCount)
    }

    Row(
        modifier = modifier
            .clip(Theme.shapes.circle)
            .background(Theme.colors.amber)
            .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall)
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = Theme.colors.onAmber,
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
        BasicText(
            text = percent,
            style = Theme.typography.body.medium.copy(color = Theme.colors.onAmber),
        )
        BasicText(
            // The denominator, so "100%" after three words reads as three words, not perfection.
            text = words,
            style = Theme.typography.body.small.copy(color = Theme.colors.onAmber.copy(alpha = 0.7f)),
        )
    }
}

/**
 * What the live session is doing right now.
 *
 * Exists because a correction session is silent by design: no mistakes means no marks, and a
 * broken connection also means no marks. Without this the two are indistinguishable, and the
 * reciter is left assuming a flawless recitation when nothing was ever checked.
 */
@Composable
fun LiveStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Theme.colors.surfaceVariant,
    contentColor: Color = Theme.colors.onSurface,
) {
    BasicText(
        text = label,
        style = Theme.typography.body.small.copy(color = contentColor),
        modifier = modifier
            .clip(Theme.shapes.circle)
            .background(containerColor)
            .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall),
    )
}
