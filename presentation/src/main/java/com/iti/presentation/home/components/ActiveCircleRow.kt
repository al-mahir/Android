package com.iti.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.presentation.R


@Composable
fun ActiveCircleRow(
    circle: Circle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.surfaceContainer)
            .border(width = 1.dp, color = Theme.colors.surfaceVariant, shape = Theme.shapes.large)
            .clickable(onClick = onClick)
            .padding(Theme.spacing.medium),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
            modifier = Modifier.weight(1f),
        ) {
            BasicText(
                text = circle.name,
                style = Theme.typography.body.large.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = stringResource(
                    R.string.circle_row_subtitle,
                    circle.host?.displayName.orEmpty(),
                    circle.currentMembers,
                    circle.maxParticipants,
                ),
                style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (circle.status == CircleStatus.ONGOING) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(Theme.shapes.small)
                    .background(Theme.colors.primary.copy(alpha = 0.12f))
                    .padding(horizontal = Theme.spacing.small, vertical = 2.dp),
            ) {
                BasicText(
                    text = stringResource(R.string.circle_status_ongoing),
                    style = Theme.typography.body.small.copy(color = Theme.colors.primary),
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Theme.colors.secondaryFont,
            modifier = Modifier.size(20.dp),
        )
    }
}
