package com.iti.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.theme.Theme
import com.iti.domain.model.home.Sheikh
import com.iti.domain.model.home.SheikhAvailability
import com.iti.presentation.R
import java.text.NumberFormat

private val CardShape = RoundedCornerShape(18.dp)
private val CardWidth = 148.dp
private val AvatarSize = 56.dp
private val StatusDotSize = 8.dp


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
            .clip(CardShape)
            .background(Theme.colors.surface)
            .border(width = 1.dp, color = Theme.colors.surfaceVariant, shape = CardShape)
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
            modifier = Modifier.size(AvatarSize),
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

        RatingRow(rating = sheikh.rating)

        AvailabilityRow(availability = sheikh.availability)
    }
}

@Composable
private fun RatingRow(rating: Double) {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]
    // Locale-aware so Arabic renders Arabic-Indic digits and the correct decimal separator.
    val formatted = remember(rating, locale) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(rating)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        Image(
            painter = painterResource(DesignSystemR.drawable.ic_star),
            contentDescription = stringResource(R.string.home_rating_content_description),
            colorFilter = ColorFilter.tint(Theme.colors.amber),
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
        BasicText(
            text = formatted,
            style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AvailabilityRow(availability: SheikhAvailability) {
    val color: Color = when (availability) {
        SheikhAvailability.AVAILABLE -> Theme.colors.success
        SheikhAvailability.IN_SESSION -> Theme.colors.error
        SheikhAvailability.OFFLINE -> Theme.colors.hint
    }
    val labelRes = when (availability) {
        SheikhAvailability.AVAILABLE -> R.string.home_status_available
        SheikhAvailability.IN_SESSION -> R.string.home_status_in_session
        SheikhAvailability.OFFLINE -> R.string.home_status_offline
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        Box(
            modifier = Modifier
                .size(StatusDotSize)
                .clip(CircleShape)
                .background(color),
        )
        BasicText(
            text = stringResource(labelRes),
            style = Theme.typography.body.small.copy(color = color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
