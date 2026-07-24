package com.iti.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.theme.Theme
import com.example.designsystem.components.avatar.InitialsAvatar
import com.iti.presentation.R


@Composable
fun HomeHeader(
    initials: String?,
    avatarUrl: String?,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Brand title — noticeably larger than before (display style)
            BasicText(
                text = stringResource(R.string.home_brand_title),
                style = Theme.typography.display.copy(
                    color = Theme.colors.primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Search icon with circular surfaceContainer background
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Theme.size.avatarSmall)
                        .clip(CircleShape)
                        .background(Theme.colors.surfaceContainer)
                        .clickable(onClick = onSearchClick)
                        .semantics { role = Role.Button },
                ) {
                    Image(
                        painter = painterResource(DesignSystemR.drawable.ic_search),
                        contentDescription = stringResource(R.string.home_search_content_description),
                        colorFilter = ColorFilter.tint(Theme.colors.primaryFont),
                        modifier = Modifier.size(Theme.size.iconMedium),
                    )
                }

                // User avatar
                InitialsAvatar(
                    initials = initials.orEmpty(),
                    contentDescription = stringResource(R.string.home_avatar_content_description),
                    imageUrl = avatarUrl,
                    modifier = Modifier
                        .size(Theme.size.avatarSmall)
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClick)
                        .semantics { role = Role.Button },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
            BasicText(
                text = stringResource(R.string.home_greeting),
                style = Theme.typography.title.copy(
                    color = Theme.colors.primary,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = stringResource(R.string.home_greeting_subtitle),
                style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
