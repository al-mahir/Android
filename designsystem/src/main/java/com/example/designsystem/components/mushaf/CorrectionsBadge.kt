package com.example.designsystem.components.mushaf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.os.ConfigurationCompat
import com.example.designsystem.R
import com.example.designsystem.theme.Theme
import java.text.NumberFormat

/**
 * A tappable pill showing how many mistakes the current session has found.
 *
 * The count is the *text* half of "colour + icon + text" (A11Y-01): the inline marks on the page
 * cannot carry a label, so this is where the mistake state becomes readable rather than merely
 * visible. Numbers are locale-formatted, so an Arabic reader sees ٢ rather than 2.
 *
 * Show it only when there is something to show — a permanent "0 mistakes" badge reads as a score,
 * and the underlying grading is not calibrated to support one.
 */


@Composable
fun CorrectionsBadge(
    count: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]
    val formatted = remember(count, locale) { NumberFormat.getIntegerInstance(locale).format(count) }

    Row(
        modifier = modifier
            .clip(Theme.shapes.circle)
            .background(Theme.colors.error)
            .clickable(onClick = onClick)
            .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall)
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            // The row carries the description; a second one would be read out twice.
            contentDescription = null,
            tint = Theme.colors.onError,
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
        BasicText(
            text = formatted,
            style = Theme.typography.body.medium.copy(color = Theme.colors.onError),
        )
    }
}
