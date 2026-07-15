package com.example.designsystem.components.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.Theme

@Composable
internal fun TopBarShowcaseScreen(modifier: Modifier = Modifier) {
    var unreadCount by remember { mutableStateOf(2) }
    var notificationCount by remember { mutableStateOf(2) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
            .verticalScroll(rememberScrollState())
            .padding(vertical = Theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            ToggleChip(
                label = "Unread = $unreadCount",
                active = unreadCount > 0,
                onClick = { unreadCount = if (unreadCount == 0) 2 else 0 },
            )
            ToggleChip(
                label = "Home badge = $notificationCount",
                active = notificationCount > 0,
                onClick = { notificationCount = if (notificationCount == 0) 2 else 0 },
            )
        }

        SectionLabel("BackTitleTopBar")
        BackTitleTopBar(
            title = stringResource(R.string.topbar_back_title),
            onBackClick = {},
        )

        SectionLabel("HomeTopBar")
        HomeTopBar(
            welcomeMessage = stringResource(R.string.topbar_home_user_name),
            onUserClick = {},
            onNotificationClick = {},
            notificationCount = notificationCount,
        )

        SectionLabel("BackTitleNotificationTopBar")
        BackTitleNotificationTopBar(
            title = stringResource(R.string.topbar_notifications_title),
            unreadCount = unreadCount,
            onBackClick = {},
            onNotificationClick = {},
        )

        Spacer(Modifier.height(Theme.spacing.medium))
    }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) Theme.colors.primary else Theme.colors.backGround
    val border = if (active) Theme.colors.primary else Theme.colors.hint
    val text = if (active) Theme.colors.onPrimary else Theme.colors.hint

    Box(
        modifier = Modifier
            .clip(Theme.shapes.small)
            .background(bg)
            .border(1.dp, border, Theme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = Theme.typography.body.small.copy(color = text),
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    BasicText(
        text = title,
        style = Theme.typography.body.large.copy(color = Theme.colors.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium),
    )
}
