package com.iti.presentation.profile.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.avatar.InitialsAvatar
import com.example.designsystem.theme.Theme
import com.iti.presentation.R

@Composable
internal fun ProfileHeader(
    displayName: String,
    email: String,
    initials: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        InitialsAvatar(
            initials = initials,
            contentDescription = stringResource(R.string.profile_avatar_content_description),
            imageUrl = avatarUrl,
            containerColor = Theme.colors.surface,
            contentColor = Theme.colors.primary,
            textStyle = Theme.typography.title,
            modifier = Modifier
                .size(Theme.size.avatarMedium)
                .border(width = 1.dp, color = Theme.colors.primary, shape = CircleShape),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
        ) {
            BasicText(
                text = displayName,
                style = Theme.typography.body.large.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            BasicText(
                text = email,
                style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
