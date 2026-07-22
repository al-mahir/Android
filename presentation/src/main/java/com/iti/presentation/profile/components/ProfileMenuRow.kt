package com.iti.presentation.profile.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.theme.Theme


@Composable
internal fun ProfileMenuRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    showChevron: Boolean = false,
    contentColor: Color = Theme.colors.primaryFont,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Theme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        BasicText(
            text = title,
            style = Theme.typography.body.large.copy(color = contentColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Theme.colors.secondaryFont,
                modifier = Modifier.size(Theme.size.iconMedium),
            )
        }

        if (showChevron) {
            Icon(
                painter = painterResource(DesignSystemR.drawable.ic_chevron_end),
                contentDescription = null,
                tint = Theme.colors.hint,
                modifier = Modifier.size(Theme.size.iconSemiMedium),
            )
        }
    }
}
