package com.iti.sheikh.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.designsystem.components.status.StatusLabel
import com.example.designsystem.theme.Theme
import com.iti.domain.model.SheikhAvailability
import com.iti.sheikh.presentation.R

/** Full status readout shown under [SheikhHomeHeader] — the header's switch is the quick toggle, this confirms what it means. */
@Composable
fun SheikhAvailabilityCard(
    availability: SheikhAvailability,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.surfaceContainer, Theme.shapes.large)
            .padding(Theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        StatusLabel(
            text = stringResource(availability.labelRes()),
            color = availability.color(),
        )

        BasicText(
            text = stringResource(availability.descriptionRes()),
            style = Theme.typography.body.medium.copy(
                color = Theme.colors.secondaryFont,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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
    SheikhAvailability.AVAILABLE -> R.string.sheikh_home_status_available
    SheikhAvailability.IN_SESSION -> R.string.sheikh_home_status_in_session
    SheikhAvailability.OFFLINE -> R.string.sheikh_home_status_offline
}

private fun SheikhAvailability.descriptionRes(): Int = when (this) {
    SheikhAvailability.AVAILABLE -> R.string.sheikh_home_status_available_description
    SheikhAvailability.IN_SESSION -> R.string.sheikh_home_status_in_session_description
    SheikhAvailability.OFFLINE -> R.string.sheikh_home_status_offline_description
}
