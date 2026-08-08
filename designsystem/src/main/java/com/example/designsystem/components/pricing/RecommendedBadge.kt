package com.example.designsystem.components.pricing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.example.designsystem.theme.Theme


@Composable
fun RecommendedBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = text,
        style = Theme.typography.hint.small.copy(
            color = Theme.colors.onPrimary,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = modifier
            .clip(Theme.shapes.small)
            .background(Theme.colors.primary)
            .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall),
    )
}
