package com.iti.presentation.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.avatar.InitialsAvatar
import com.example.designsystem.theme.Theme
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.presentation.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CircleCard(
    circle: Circle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isJoined: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.surface)
            .border(width = 1.dp, color = Theme.colors.surfaceVariant, shape = Theme.shapes.large)
            .clickable(onClick = onClick)
            .padding(Theme.spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            InitialsAvatar(
                initials = circleInitials(circle),
                contentDescription = circle.name,
                modifier = Modifier.size(Theme.size.avatarSmall),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
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
                circle.host?.displayName?.takeIf { it.isNotBlank() }?.let { hostName ->
                    BasicText(
                        text = hostName,
                        style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            CircleStatusPill(status = circle.status)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Theme.colors.secondaryFont,
                modifier = Modifier.size(20.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircleTypeBadge(type = circle.type)
            if (isJoined) JoinedBadge()

            val date = circleDateText(circle.startDate)
            if (date != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Theme.colors.secondaryFont,
                        modifier = Modifier.size(16.dp),
                    )
                    BasicText(
                        text = date,
                        style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = null,
                    tint = Theme.colors.secondaryFont,
                    modifier = Modifier.size(16.dp),
                )
                BasicText(
                    text = stringResource(
                        R.string.circle_row_members,
                        circle.currentMembers,
                        circle.maxParticipants,
                    ),
                    style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                )
            }
        }
    }
}

@Composable
private fun CircleStatusPill(status: CircleStatus) {
    val label = when (status) {
        CircleStatus.SCHEDULED -> stringResource(R.string.circle_status_scheduled)
        CircleStatus.ONGOING -> stringResource(R.string.circle_status_ongoing)
        CircleStatus.COMPLETED -> stringResource(R.string.circle_status_completed)
        CircleStatus.CANCELLED -> stringResource(R.string.circle_status_cancelled)
    }
    val foreground = when (status) {
        CircleStatus.ONGOING -> Theme.colors.primary
        CircleStatus.CANCELLED -> Theme.colors.error
        else -> Theme.colors.secondaryFont
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(Theme.shapes.small)
            .background(if (status == CircleStatus.ONGOING) Theme.colors.primary.copy(alpha = 0.12f) else Theme.colors.surfaceVariant)
            .padding(horizontal = Theme.spacing.small, vertical = 2.dp),
    ) {
        if (status == CircleStatus.ONGOING) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(Theme.shapes.small)
                    .background(Theme.colors.primary),
            )
        }
        BasicText(
            text = label,
            style = Theme.typography.body.small.copy(color = foreground),
        )
    }
}

@Composable
private fun CircleTypeBadge(type: CircleType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(Theme.shapes.small)
            .background(Theme.colors.surfaceVariant)
            .padding(horizontal = Theme.spacing.small, vertical = 3.dp),
    ) {
        Icon(
            imageVector = when (type) {
                CircleType.PUBLIC -> Icons.Outlined.Public
                CircleType.PRIVATE -> Icons.Outlined.Lock
            },
            contentDescription = null,
            tint = Theme.colors.secondaryFont,
            modifier = Modifier.size(14.dp),
        )
        BasicText(
            text = when (type) {
                CircleType.PUBLIC -> stringResource(R.string.circle_type_public)
                CircleType.PRIVATE -> stringResource(R.string.circle_type_private)
            },
            style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
        )
    }
}

@Composable
private fun JoinedBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(Theme.shapes.small)
            .background(Theme.colors.primary.copy(alpha = 0.12f))
            .padding(horizontal = Theme.spacing.small, vertical = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(Theme.shapes.small)
                .background(Theme.colors.primary),
        )
        BasicText(
            text = stringResource(R.string.circle_joined),
            style = Theme.typography.body.small.copy(color = Theme.colors.primary),
        )
    }
}

@Composable
internal fun circleInitials(circle: Circle): String {
    circle.host?.initials?.takeIf { it.isNotBlank() }?.let { return it }
    return circle.name
        .split(" ")
        .mapNotNull { it.firstOrNull() }
        .take(2)
        .joinToString("")
        .uppercase()
}

@Composable
internal fun circleDateText(iso: String): String? = runCatching {
    DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(javaLocale(Locale.current))
        .withZone(ZoneId.systemDefault())
        .format(Instant.parse(iso))
}.getOrNull()

private fun javaLocale(locale: Locale): java.util.Locale =
    if (locale.region.isBlank()) java.util.Locale(locale.language)
    else java.util.Locale(locale.language, locale.region)
