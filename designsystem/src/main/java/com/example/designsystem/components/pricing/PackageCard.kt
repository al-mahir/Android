package com.example.designsystem.components.pricing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.theme.Theme


@Composable
fun PackageCard(
    title: String,
    priceText: String,
    pricePeriodText: String,
    features: List<String>,
    selectCaption: String,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRecommended: Boolean = false,
    recommendedLabel: String = "",
    isLoading: Boolean = false,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = Theme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = Theme.colors.primary.copy(alpha = 0.08f),
            ),
            border = BorderStroke(1.dp, Theme.colors.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Theme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                BasicText(
                    text = title,
                    style = Theme.typography.title.copy(color = Theme.colors.primaryFont),
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    BasicText(
                        text = priceText,
                        style = Theme.typography.display.copy(
                            color = Theme.colors.primary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    BasicText(
                        text = pricePeriodText,
                        style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
                        modifier = Modifier.padding(start = Theme.spacing.extraSmall, bottom = Theme.spacing.extraSmall),
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                    modifier = Modifier.padding(top = Theme.spacing.extraSmall),
                ) {
                    features.forEach { feature ->
                        FeatureRow(text = feature)
                    }
                }

                PrimaryButton(
                    caption = selectCaption,
                    onClick = onSelectClick,
                    isLoading = isLoading,
                    shape = Theme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Theme.spacing.small),
                )
            }
        }

        if (isRecommended) {
            RecommendedBadge(
                text = recommendedLabel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Theme.spacing.small),
            )
        }
    }
}

@Composable
private fun FeatureRow(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = Theme.colors.primary,
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
        BasicText(
            text = text,
            style = Theme.typography.body.small.copy(color = Theme.colors.primaryFont),
        )
    }
}
