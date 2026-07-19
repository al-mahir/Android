package com.example.designsystem.components.rating

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.os.ConfigurationCompat
import com.example.designsystem.R
import com.example.designsystem.theme.Theme
import java.text.NumberFormat


@Composable
fun RatingLabel(
    rating: Double,
    contentDescription: String,
    modifier: Modifier = Modifier,
    starColor: Color = Theme.colors.amber,
    textColor: Color = Theme.colors.secondaryFont,
) {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]
    val formatted = remember(rating, locale) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(rating)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_star),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(starColor),
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
        BasicText(
            text = formatted,
            style = Theme.typography.body.small.copy(color = textColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
