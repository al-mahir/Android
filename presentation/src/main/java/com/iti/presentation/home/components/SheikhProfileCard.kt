package com.iti.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.avatar.InitialsAvatar
import com.example.designsystem.components.rating.RatingLabel
import com.example.designsystem.components.status.StatusLabel
import com.example.designsystem.theme.Theme
import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability
import com.iti.presentation.R

private val CardWidth = 148.dp

@Composable
fun SheikhProfileCard(
    sheikh: Sheikh,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        modifier = modifier
            .width(CardWidth)
            .clip(Theme.shapes.large)
            .background(Theme.colors.surface)
            .border(width = 1.dp, color = Theme.colors.surfaceVariant, shape = Theme.shapes.large)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(Theme.spacing.medium),
    ) {
        InitialsAvatar(
            initials = sheikh.initials,
            contentDescription = stringResource(
                R.string.home_sheikh_avatar_content_description,
                sheikh.name,
            ),
            imageUrl = sheikh.avatarUrl,
            textStyle = Theme.typography.body.large,
            modifier = Modifier.size(Theme.size.avatarMedium),
        )

        BasicText(
            text = sheikh.name,
            style = Theme.typography.body.large.copy(
                color = Theme.colors.primaryFont,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        RatingLabel(
            rating = sheikh.rating,
            contentDescription = stringResource(R.string.home_rating_content_description),
        )

        StatusLabel(
            text = stringResource(sheikh.availability.labelRes()),
            color = sheikh.availability.color(),
        )
    }
}

@Composable
private fun SheikhAvailability.color(): Color = when (this) {
    SheikhAvailability.AVAILABLE -> Theme.colors.success
    SheikhAvailability.IN_SESSION -> Theme.colors.error
    SheikhAvailability.OFFLINE -> Theme.colors.hint
}

private fun SheikhAvailability.labelRes(): Int = when (this) {
    SheikhAvailability.AVAILABLE -> R.string.home_status_available
    SheikhAvailability.IN_SESSION -> R.string.home_status_in_session
    SheikhAvailability.OFFLINE -> R.string.home_status_offline
}
