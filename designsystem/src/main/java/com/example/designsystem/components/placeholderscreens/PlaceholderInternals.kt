package com.example.designsystem.components.placeholderscreens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

/**
 * 160-dp tinted illustration used by every placeholder screen so the
 * monochrome vector drawables follow the active color scheme.
 */
@Composable
internal fun PlaceholderIcon(
    @DrawableRes resId: Int,
    size: Dp = 160.dp,
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier.size(size),
        colorFilter = ColorFilter.tint(Theme.colors.hint),
    )
}

@Composable
internal fun PlaceholderTitle(text: String) {
    BasicText(
        text = text,
        style = Theme.typography.title.copy(
            color = Theme.colors.primaryFont,
            textAlign = TextAlign.Center,
        ),
    )
}

@Composable
internal fun PlaceholderDescription(text: String) {
    BasicText(
        text = text,
        style = Theme.typography.body.medium.copy(
            color = Theme.colors.secondaryFont,
            textAlign = TextAlign.Center,
        ),
    )
}
