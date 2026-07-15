package com.example.designsystem.components.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

// ─── Showcase (deployable) ────────────────────────────────────────────────────

@Preview(name = "🖥 All TopBars Showcase – Light", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewTopBarShowcaseLight() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        TopBarShowcaseScreen()
    }
}

@Preview(name = "🖥 All TopBars Showcase – Dark RTL", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewTopBarShowcaseDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        TopBarShowcaseScreen()
    }
}

// ─── BackTitleTopBar ──────────────────────────────────────────────────────────

@Preview(name = "BackTitleTopBar – Light LTR", showBackground = true, group = "BackTitleTopBar")
@Composable
private fun PreviewBackTitleLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        TopBarPreviewScaffold { BackTitleAllStyles() }
    }
}

@Preview(name = "BackTitleTopBar – Light RTL", showBackground = true, group = "BackTitleTopBar")
@Composable
private fun PreviewBackTitleLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        TopBarPreviewScaffold { BackTitleAllStyles() }
    }
}

@Preview(name = "BackTitleTopBar – Dark LTR", showBackground = true, group = "BackTitleTopBar")
@Composable
private fun PreviewBackTitleDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) {
        TopBarPreviewScaffold { BackTitleAllStyles() }
    }
}

@Preview(name = "BackTitleTopBar – Dark RTL", showBackground = true, group = "BackTitleTopBar")
@Composable
private fun PreviewBackTitleDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        TopBarPreviewScaffold { BackTitleAllStyles() }
    }
}

// ─── HomeTopBar ───────────────────────────────────────────────────────────────

@Preview(name = "HomeTopBar – Light LTR", showBackground = true, group = "HomeTopBar")
@Composable
private fun PreviewHomeLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        TopBarPreviewScaffold { HomeAllStyles() }
    }
}

@Preview(name = "HomeTopBar – Light RTL", showBackground = true, group = "HomeTopBar")
@Composable
private fun PreviewHomeLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        TopBarPreviewScaffold { HomeAllStyles() }
    }
}

@Preview(name = "HomeTopBar – Dark LTR", showBackground = true, group = "HomeTopBar")
@Composable
private fun PreviewHomeDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) {
        TopBarPreviewScaffold { HomeAllStyles() }
    }
}

@Preview(name = "HomeTopBar – Dark RTL", showBackground = true, group = "HomeTopBar")
@Composable
private fun PreviewHomeDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        TopBarPreviewScaffold { HomeAllStyles() }
    }
}

// ─── BackTitleNotificationTopBar ──────────────────────────────────────────────

@Preview(name = "BackTitleNotificationTopBar – Light LTR", showBackground = true, group = "BackTitleNotificationTopBar")
@Composable
private fun PreviewBackTitleNotifLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        TopBarPreviewScaffold { BackTitleNotificationAllStates() }
    }
}

@Preview(name = "BackTitleNotificationTopBar – Light RTL", showBackground = true, group = "BackTitleNotificationTopBar")
@Composable
private fun PreviewBackTitleNotifLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        TopBarPreviewScaffold { BackTitleNotificationAllStates() }
    }
}

@Preview(name = "BackTitleNotificationTopBar – Dark LTR", showBackground = true, group = "BackTitleNotificationTopBar")
@Composable
private fun PreviewBackTitleNotifDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) {
        TopBarPreviewScaffold { BackTitleNotificationAllStates() }
    }
}

@Preview(name = "BackTitleNotificationTopBar – Dark RTL", showBackground = true, group = "BackTitleNotificationTopBar")
@Composable
private fun PreviewBackTitleNotifDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        TopBarPreviewScaffold { BackTitleNotificationAllStates() }
    }
}

// ─── Combined ─────────────────────────────────────────────────────────────────

@Preview(name = "All TopBars – Light LTR", showBackground = true, group = "AllTopBars")
@Composable
private fun PreviewAllLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        TopBarPreviewScaffold { AllVariantsCombined() }
    }
}

@Preview(name = "All TopBars – Dark RTL", showBackground = true, group = "AllTopBars")
@Composable
private fun PreviewAllDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        TopBarPreviewScaffold { AllVariantsCombined() }
    }
}

// ─── Reusable state blocks ────────────────────────────────────────────────────

@Composable
private fun BackTitleAllStyles() {
    StateLabel("Default (circular semi-transparent)")
    BackTitleTopBar(
        title = stringResource(R.string.topbar_back_title),
        onBackClick = {},
    )
    Spacer(Modifier.height(8.dp))
    StateLabel("Plain icon")
    BackTitleTopBar(
        title = stringResource(R.string.topbar_back_title),
        onBackClick = {},
        iconStyle = TopBarIconStyle.Plain,
    )
    Spacer(Modifier.height(8.dp))
    StateLabel("Gradient background + decoration")
    BackTitleTopBar(
        title = stringResource(R.string.topbar_back_title),
        onBackClick = {},
        decoration = {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
            )
        },
    )
}

@Composable
private fun HomeAllStyles() {
    StateLabel("With unread badge = 2")
    HomeTopBar(
        welcomeMessage = stringResource(R.string.topbar_home_user_name),
        onUserClick = {},
        onNotificationClick = {},
        notificationCount = 2,
    )
    Spacer(Modifier.height(8.dp))
    StateLabel("No badge")
    HomeTopBar(
        welcomeMessage = stringResource(R.string.topbar_home_user_name),
        onUserClick = {},
        onNotificationClick = {},
        notificationCount = 0,
    )
}

@Composable
private fun BackTitleNotificationAllStates() {
    StateLabel("With unread = 2 (default styles)")
    BackTitleNotificationTopBar(
        title = stringResource(R.string.topbar_notifications_title),
        unreadCount = 2,
        onBackClick = {},
        onNotificationClick = {},
    )
    Spacer(Modifier.height(8.dp))
    StateLabel("No unread (count = 0)")
    BackTitleNotificationTopBar(
        title = stringResource(R.string.topbar_notifications_title),
        unreadCount = 0,
        onBackClick = {},
        onNotificationClick = {},
    )
    Spacer(Modifier.height(8.dp))
    StateLabel("Both icons circular + count = 5")
    BackTitleNotificationTopBar(
        title = stringResource(R.string.topbar_notifications_title),
        unreadCount = 5,
        onBackClick = {},
        onNotificationClick = {},
        notificationIconStyle = TopBarIconStyle.CircularBackground(),
    )
}

@Composable
private fun AllVariantsCombined() {
    StateLabel("BackTitleTopBar")
    BackTitleTopBar(
        title = stringResource(R.string.topbar_back_title),
        onBackClick = {},
    )
    Spacer(Modifier.height(8.dp))
    StateLabel("HomeTopBar")
    HomeTopBar(
        welcomeMessage = stringResource(R.string.topbar_home_user_name),
        onUserClick = {},
        onNotificationClick = {},
        notificationCount = 2,
    )
    Spacer(Modifier.height(8.dp))
    StateLabel("BackTitleNotificationTopBar")
    BackTitleNotificationTopBar(
        title = stringResource(R.string.topbar_notifications_title),
        unreadCount = 2,
        onBackClick = {},
        onNotificationClick = {},
    )
}

// ─── Layout helpers ───────────────────────────────────────────────────────────

@Composable
private fun TopBarPreviewScaffold(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        content()
    }
}

@Composable
private fun StateLabel(text: String) {
    BasicText(
        text = text,
        style = Theme.typography.body.small.copy(color = Theme.colors.hint),
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
