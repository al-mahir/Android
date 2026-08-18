package com.iti.presentation.payment.checkout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.components.card.GradientCard
import com.example.designsystem.theme.Theme
import com.iti.domain.model.SubscriptionPackage
import com.iti.presentation.R
import com.iti.presentation.subscription.components.billingPeriodLabel

@Composable
internal fun PackageSummaryCard(pkg: SubscriptionPackage, modifier: Modifier = Modifier) {
    val priceText = rememberFormattedWholePrice(pkg.priceAmount, pkg.currencyCode)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Theme.spacing.small),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(DesignSystemR.drawable.ic_check),
                contentDescription = null,
                tint = Theme.colors.secondaryFont,
                modifier = Modifier.padding(end = Theme.spacing.extraSmall),
            )
            Text(
                text = stringResource(R.string.checkout_secured_by_paymob),
                style = Theme.typography.body.small,
                color = Theme.colors.secondaryFont,
            )
        }

        GradientCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.checkout_reciter_pass_eyebrow),
                        style = Theme.typography.body.small,
                    )
                    Text(text = pkg.name, style = Theme.typography.title.copy(fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = priceText, style = Theme.typography.title.copy(fontWeight = FontWeight.Bold))
                    val periodLabel = billingPeriodLabel(pkg.durationDays)
                    if (periodLabel.isNotBlank()) {
                        Text(text = periodLabel, style = Theme.typography.body.small)
                    }
                }
            }

            Column(modifier = Modifier.padding(top = Theme.spacing.medium)) {
                pkg.features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = Theme.spacing.extraSmall),
                    ) {
                        Icon(
                            painter = painterResource(DesignSystemR.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.padding(end = Theme.spacing.small),
                        )
                        Text(text = feature, style = Theme.typography.body.small)
                    }
                }
            }
        }
    }
}
