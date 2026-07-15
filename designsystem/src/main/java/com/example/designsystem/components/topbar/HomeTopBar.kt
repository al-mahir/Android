package com.example.designsystem.components.topbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.Theme

@Composable
fun HomeTopBar(
    welcomeMessage: String,
    onUserClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
    notificationCount: Int = 0,
    userAvatar: Painter = painterResource(R.drawable.ic_profile),
    notificationIcon: Painter = painterResource(R.drawable.ic_notifications),
    isUserAvatarPlaceholder: Boolean = true,
) {
    BaseTopBar(
        modifier = modifier,
        backgroundColor = SolidColor(Color.Transparent),
        shape = RectangleShape,
        decoration = {},
        start = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopBarUserAvatar(
                    avatar = userAvatar,
                    contentDescription = stringResource(R.string.topbar_user_content_description),
                    onClick = onUserClick,
                    style = TopBarIconStyle.CircularBackground(
                        backgroundColor = Color.White,
                        containerSize = 36.dp,
                        iconSize = 32.dp,
                    ),
                    tint = if (isUserAvatarPlaceholder) Theme.colors.primary else null,
                )
                BasicText(
                    text = welcomeMessage,
                    style = Theme.typography.body.medium.copy(
                        color = Theme.colors.onPrimary,
                        textAlign = TextAlign.End,
                    ),
                    maxLines = 2,
                    modifier = Modifier.widthIn(max = 160.dp),
                )
            }
        },
        end = {
            TopBarIcon(
                icon = notificationIcon,
                contentDescription = stringResource(R.string.topbar_notifications_content_description),
                onClick = onNotificationClick,
                style = TopBarIconStyle.CircularBackground(
                    backgroundColor = Color.White,
                    tint = Theme.colors.primary,
                    containerSize = 44.dp,
                    iconSize = 20.dp,
                ),
                badgeCount = notificationCount,
            )
        },
    )
}
