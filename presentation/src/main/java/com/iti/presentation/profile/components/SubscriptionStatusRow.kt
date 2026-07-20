package com.iti.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.designsystem.theme.Theme
import com.iti.presentation.R


@Composable
internal fun SubscriptionStatusRow(
    isPremium: Boolean,
    joinedAtEpochMillis: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        MetaColumn(
            label = stringResource(R.string.profile_subscription_status),
            value = stringResource(
                if (isPremium) R.string.profile_plan_premium else R.string.profile_plan_none
            ),
            alignment = Alignment.Start,
            textAlign = TextAlign.Start,
        )

        MetaColumn(
            label = stringResource(R.string.profile_joined),
            value = rememberFormattedDate(joinedAtEpochMillis),
            alignment = Alignment.End,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun MetaColumn(
    label: String,
    value: String,
    alignment: Alignment.Horizontal,
    textAlign: TextAlign,
) {
    Column(
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        BasicText(
            text = label,
            style = Theme.typography.body.small.copy(
                color = Theme.colors.secondaryFont,
                textAlign = textAlign,
            ),
        )

        BasicText(
            text = value,
            style = Theme.typography.body.medium.copy(
                color = Theme.colors.primaryFont,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
            ),
        )
    }
}
