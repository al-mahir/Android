package com.iti.presentation.profile.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.card.InnerContentCard
import com.example.designsystem.theme.Theme
import com.iti.presentation.R

/** Shown when the student has no subscription at all: explains the gate and links to packages. */
@Composable
internal fun NoSubscriptionCard(
    onBrowsePackagesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InnerContentCard(modifier = modifier) {
        BasicText(
            text = stringResource(R.string.profile_no_subscription_title),
            style = Theme.typography.body.medium.copy(
                color = Theme.colors.primaryFont,
                fontWeight = FontWeight.Bold,
            ),
        )
        BasicText(
            text = stringResource(R.string.profile_no_subscription_description),
            style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
            modifier = Modifier.padding(top = Theme.spacing.extraSmall),
        )
        PrimaryButton(
            caption = stringResource(R.string.profile_no_subscription_cta),
            onClick = onBrowsePackagesClick,
            shape = Theme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Theme.spacing.medium),
        )
    }
}
