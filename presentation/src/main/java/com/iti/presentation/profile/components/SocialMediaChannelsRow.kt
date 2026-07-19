package com.iti.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Icon
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.profile.model.SocialChannel

@Composable
internal fun SocialMediaChannelsRow(
    onChannelClick: (SocialChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = Theme.spacing.small,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialChannel.entries.forEach { channel ->
            SocialChannelBadge(
                channel = channel,
                onClick = { onChannelClick(channel) },
            )
        }
    }
}

@Composable
private fun SocialChannelBadge(
    channel: SocialChannel,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(Theme.size.avatarSmall)
            .clip(CircleShape)
            .background(Theme.colors.surface)
            .clickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(channel.iconRes),
            contentDescription = stringResource(channel.labelRes),
            tint = Theme.colors.secondaryFont,
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
    }
}


private val SocialChannel.iconRes: Int
    get() = when (this) {
        SocialChannel.GITHUB -> DesignSystemR.drawable.ic_social_github
        SocialChannel.DISCORD -> DesignSystemR.drawable.ic_social_discord
        SocialChannel.X -> DesignSystemR.drawable.ic_social_x
        SocialChannel.FACEBOOK -> DesignSystemR.drawable.ic_social_facebook
        SocialChannel.YOUTUBE -> DesignSystemR.drawable.ic_social_youtube
        SocialChannel.INSTAGRAM -> DesignSystemR.drawable.ic_social_instagram
    }

private val SocialChannel.labelRes: Int
    get() = when (this) {
        SocialChannel.GITHUB -> R.string.profile_social_github
        SocialChannel.DISCORD -> R.string.profile_social_discord
        SocialChannel.X -> R.string.profile_social_x
        SocialChannel.FACEBOOK -> R.string.profile_social_facebook
        SocialChannel.YOUTUBE -> R.string.profile_social_youtube
        SocialChannel.INSTAGRAM -> R.string.profile_social_instagram
    }
