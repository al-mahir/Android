package com.example.designsystem.components.topbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.designsystem.theme.Theme

sealed interface TopBarIconStyle {
    data object Plain : TopBarIconStyle

    data class CircularBackground(
        val backgroundColor: Color? = null,
        val tint: Color? = null,
        val containerSize: Dp = 40.dp,
        val iconSize: Dp = 20.dp,
    ) : TopBarIconStyle
}

@Composable
internal fun TopBarIcon(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TopBarIconStyle = TopBarIconStyle.Plain,
    tint: Color = Theme.colors.onPrimary,
    badgeCount: Int? = null,
) {
    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.TopEnd,
    ) {
        when (style) {
            TopBarIconStyle.Plain -> {
                Box(
                    modifier = Modifier
                        .size(Theme.size.iconMedium)
                        .clip(Theme.shapes.circle)
                        .clickable(role = Role.Button, onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(Theme.size.iconMedium),
                        colorFilter = ColorFilter.tint(tint),
                    )
                }
            }

            is TopBarIconStyle.CircularBackground -> {
                val bg = style.backgroundColor ?: Theme.colors.onPrimary.copy(alpha = 0.18f)
                val iconTint = style.tint ?: tint
                Box(
                    modifier = Modifier
                        .size(style.containerSize)
                        .clip(Theme.shapes.circle)
                        .background(bg)
                        .clickable(role = Role.Button, onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(style.iconSize),
                        colorFilter = ColorFilter.tint(iconTint),
                    )
                }
            }
        }

        if (badgeCount != null && badgeCount > 0) {
            NotificationBadge(
                count = badgeCount,
                modifier = Modifier.offset(x = 4.dp, y = (-4).dp),
            )
        }
    }
}

@Composable
private fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val text = if (count > 9) "9+" else count.toString()
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
            .clip(Theme.shapes.circle)
            .background(Theme.colors.error)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = Theme.typography.body.small.copy(
                color = Theme.colors.onError,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
internal fun TopBarUserAvatar(
    avatar: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TopBarIconStyle = TopBarIconStyle.CircularBackground(),
    tint: Color? = null,
) {
    val containerSize: Dp
    val backgroundColor: Color
    val avatarSize: Dp
    when (style) {
        TopBarIconStyle.Plain -> {
            containerSize = Theme.size.iconLarge
            backgroundColor = Color.Transparent
            avatarSize = Theme.size.iconLarge
        }
        is TopBarIconStyle.CircularBackground -> {
            containerSize = style.containerSize
            backgroundColor = style.backgroundColor ?: Theme.colors.onPrimary.copy(alpha = 0.18f)
            avatarSize = style.iconSize
        }
    }

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(Theme.shapes.circle)
            .background(backgroundColor)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = avatar,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(if (tint == null) containerSize else avatarSize)
                .clip(Theme.shapes.circle),
            contentScale = if (tint == null) ContentScale.Crop else ContentScale.Fit,
            colorFilter = tint?.let { ColorFilter.tint(it) },
        )
    }
}
