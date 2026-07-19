package com.example.designsystem.components.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.example.designsystem.theme.Theme


@Composable
fun InitialsAvatar(
    initials: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    containerColor: Color = Theme.colors.primary,
    contentColor: Color = Theme.colors.onPrimary,
    textStyle: TextStyle = Theme.typography.body.medium,
) {
    val description = contentDescription

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .semantics { this.contentDescription = description },
    ) {
        BasicText(
            text = initials,
            style = textStyle.copy(color = contentColor, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
