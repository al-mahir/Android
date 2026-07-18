package com.iti.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.theme.Theme
import com.iti.presentation.R

private val CardShape = RoundedCornerShape(20.dp)
private val IconBoxShape = RoundedCornerShape(14.dp)
private val IconBoxSize = 52.dp

private const val SupportingAlpha = 0.75f
private const val IconBoxAlpha = 0.15f


@Composable
fun ContinueReadingCard(
    surahName: String,
    ayahNumber: Int,
    pageNumber: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Theme.colors.primary)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(Theme.spacing.medium),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(IconBoxSize)
                .clip(IconBoxShape)
                .background(Theme.colors.onPrimary.copy(alpha = IconBoxAlpha)),
        ) {
            Image(
                painter = painterResource(DesignSystemR.drawable.ic_book),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Theme.colors.onPrimary),
                modifier = Modifier.size(Theme.size.iconMedium),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
            modifier = Modifier.weight(1f),
        ) {
            BasicText(
                text = stringResource(R.string.home_last_read),
                style = Theme.typography.body.small.copy(
                    color = Theme.colors.onPrimary.copy(alpha = SupportingAlpha),
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = surahName,
                style = Theme.typography.title.copy(
                    color = Theme.colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = stringResource(R.string.home_ayah_page, ayahNumber, pageNumber),
                style = Theme.typography.body.small.copy(
                    color = Theme.colors.onPrimary.copy(alpha = SupportingAlpha),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
