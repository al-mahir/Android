package com.example.designsystem.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.designsystem.theme.Theme

@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Theme.size.statusDot)
            .clip(CircleShape)
            .background(color),
    )
}


@Composable
fun StatusLabel(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        StatusDot(color = color, modifier = Modifier.clearAndSetSemantics {})
        BasicText(
            text = text,
            style = Theme.typography.body.small.copy(color = color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
