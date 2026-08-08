package com.iti.presentation.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.avatar.InitialsAvatar
import com.example.designsystem.theme.Theme
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.presentation.R

/** Prominent entry for the circle the current user belongs to — pinned above the circle list and
 * replacing the plain circles summary on Home. Tapping opens the circle details. */
@Composable
fun CurrentCircleCard(
    circle: Circle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.primary)
            .clickable(onClick = onClick)
            .padding(Theme.spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(Theme.shapes.small)
                    .background(Theme.colors.onPrimary.copy(alpha = 0.16f))
                    .padding(horizontal = Theme.spacing.small, vertical = 3.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = null,
                    tint = Theme.colors.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
                BasicText(
                    text = stringResource(R.string.current_circle_title),
                    style = Theme.typography.body.small.copy(color = Theme.colors.onPrimary),
                )
            }
            BasicText(
                text = circleStatusLabel(circle.status),
                style = Theme.typography.body.small.copy(color = Theme.colors.onPrimary.copy(alpha = 0.8f)),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            InitialsAvatar(
                initials = circleInitials(circle),
                contentDescription = circle.name,
                containerColor = Theme.colors.onPrimary,
                contentColor = Theme.colors.primary,
                modifier = Modifier.size(Theme.size.avatarMedium),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                BasicText(
                    text = circle.name,
                    style = Theme.typography.body.large.copy(
                        color = Theme.colors.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                circle.host?.displayName?.takeIf { it.isNotBlank() }?.let { hostName ->
                    BasicText(
                        text = hostName,
                        style = Theme.typography.body.small.copy(color = Theme.colors.onPrimary.copy(alpha = 0.8f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Theme.colors.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val date = circleDateText(circle.startDate)
            if (date != null) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = Theme.colors.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp),
                )
                BasicText(
                    text = date,
                    style = Theme.typography.body.small.copy(color = Theme.colors.onPrimary.copy(alpha = 0.8f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            BasicText(
                text = stringResource(
                    R.string.circle_row_members,
                    circle.currentMembers,
                    circle.maxParticipants,
                ),
                style = Theme.typography.body.small.copy(color = Theme.colors.onPrimary.copy(alpha = 0.8f)),
            )
        }
    }
}

@Composable
private fun circleStatusLabel(status: CircleStatus): String = when (status) {
    CircleStatus.SCHEDULED -> stringResource(R.string.circle_status_scheduled)
    CircleStatus.ONGOING -> stringResource(R.string.circle_status_ongoing)
    CircleStatus.COMPLETED -> stringResource(R.string.circle_status_completed)
    CircleStatus.CANCELLED -> stringResource(R.string.circle_status_cancelled)
}
