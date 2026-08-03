package com.iti.presentation.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.presentation.R
import com.iti.presentation.sheikh.SheikhInitialsAvatar

@Composable
fun CircleCard(
    circle: Circle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = circle.name,
                style = Theme.typography.body.large,
                color = Theme.colors.onSurface,
            )
            if (circle.status == CircleStatus.ONGOING) LiveBadge()
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            StatusBadge(status = circle.status)
            TypeBadge(type = circle.type)
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheikhInitialsAvatar(
                initials = circle.host?.initials.orEmpty(),
                sheikhId = circle.host?.userId.orEmpty(),
                size = 36,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circle.host?.displayName.orEmpty(),
                    style = Theme.typography.body.medium,
                    color = Theme.colors.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.circle_participants_label,
                        circle.currentMembers,
                        circle.maxParticipants,
                    ),
                    style = Theme.typography.body.small,
                    color = Theme.colors.secondaryFont,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Theme.colors.secondaryFont,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LiveBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Theme.colors.error.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Theme.colors.error),
        )
        Text(text = "LIVE", style = Theme.typography.body.small, color = Theme.colors.error)
    }
}

@Composable
private fun StatusBadge(status: CircleStatus) {
    val label = when (status) {
        CircleStatus.SCHEDULED -> stringResource(R.string.circle_status_scheduled)
        CircleStatus.ONGOING -> stringResource(R.string.circle_status_ongoing)
        CircleStatus.COMPLETED -> stringResource(R.string.circle_status_completed)
        CircleStatus.CANCELLED -> stringResource(R.string.circle_status_cancelled)
    }
    Badge(text = label)
}

@Composable
private fun TypeBadge(type: CircleType) {
    val label = when (type) {
        CircleType.PUBLIC -> stringResource(R.string.circle_type_public)
        CircleType.PRIVATE -> stringResource(R.string.circle_type_private)
    }
    Badge(text = label)
}

@Composable
private fun Badge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Theme.colors.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, style = Theme.typography.body.small, color = Theme.colors.secondaryFont)
    }
}
