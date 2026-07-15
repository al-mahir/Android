package com.example.designsystem.components.topbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.designsystem.R
import com.example.designsystem.theme.Theme

@Composable
fun BackTitleNotificationTopBar(
    title: String,
    unreadCount: Int,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
    unreadLabel: String? = if (unreadCount > 0)
        stringResource(R.string.topbar_unread_notifications, unreadCount)
    else null,
    backIcon: Painter = painterResource(R.drawable.ic_arrow_back),
    notificationIcon: Painter = painterResource(R.drawable.ic_notifications),
    backIconStyle: TopBarIconStyle = TopBarIconStyle.CircularBackground(),
    notificationIconStyle: TopBarIconStyle = TopBarIconStyle.Plain,
) {
    BaseTopBar(
        modifier = modifier,
        start = {
            TopBarIcon(
                icon = backIcon,
                contentDescription = stringResource(R.string.topbar_back_content_description),
                onClick = onBackClick,
                style = backIconStyle,
            )
        },
        center = {
            BasicText(
                text = title,
                style = Theme.typography.title.copy(
                    color = Theme.colors.onPrimary,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
            )
        },
        end = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopBarIcon(
                    icon = notificationIcon,
                    contentDescription = stringResource(R.string.topbar_notifications_content_description),
                    onClick = onNotificationClick,
                    style = notificationIconStyle,
                )
                if (!unreadLabel.isNullOrBlank()) {
                    BasicText(
                        text = unreadLabel,
                        style = Theme.typography.body.medium.copy(color = Theme.colors.onPrimary),
                        maxLines = 2,
                    )
                }
            }
        },
    )
}
